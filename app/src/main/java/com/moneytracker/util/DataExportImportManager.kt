package com.moneytracker.util

import android.content.Context
import com.moneytracker.data.local.entity.CategoryEntity
import com.moneytracker.data.local.entity.DetailEntity
import com.moneytracker.data.local.entity.GroceryBudgetItemEntity
import com.moneytracker.data.local.entity.RecurrenceFrequency
import com.moneytracker.data.local.entity.ShoppingListEntity
import com.moneytracker.data.local.entity.ShoppingListItemEntity
import com.moneytracker.data.local.entity.SubCategoryEntity
import com.moneytracker.data.local.entity.TransactionEntity
import com.moneytracker.data.local.entity.TransactionType
import com.moneytracker.data.local.entity.UnitSizeEntity
import com.moneytracker.data.repository.TransactionRepository
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class ExportFormat(val extension: String, val mimeType: String, val displayName: String) {
    JSON("json", "application/json", "JSON (.json)"),
    XML("xml", "application/xml", "XML (.xml)"),
    CSV("csv", "text/csv", "CSV (.csv)"),
    EXCEL("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel Workbook (.xlsx)")
}

data class ImportResult(
    val success: Boolean,
    val transactionsImported: Int = 0,
    val groceriesImported: Int = 0,
    val shoppingListsImported: Int = 0,
    val message: String = ""
)

object DataExportImportManager {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    // =========================================================================
    // 1. EXPORT ROUTER
    // =========================================================================
    suspend fun exportData(
        repository: TransactionRepository,
        format: ExportFormat,
        outputStream: OutputStream
    ) {
        val pid = repository.activeProfileId
        val transactions = repository.observeAllTransactions().firstOrNull() ?: emptyList()
        val groceryBudget = repository.getAllGroceryBudgetItemsForProfile(pid)
        val categories = repository.observeCategories(TransactionType.EXPENSE).firstOrNull() ?: emptyList()
        val allCategories = repository.getAllCategoryEntities(pid)
        val allSubCategories = repository.getAllSubCategoryEntities(pid)
        val allDetails = repository.getAllDetailEntities(pid)
        val unitSizes = repository.observeUnitSizes().firstOrNull() ?: emptyList()

        when (format) {
            ExportFormat.JSON -> exportJson(transactions, groceryBudget, allCategories, allSubCategories, allDetails, unitSizes, outputStream)
            ExportFormat.XML -> exportXml(transactions, groceryBudget, allCategories, allSubCategories, allDetails, unitSizes, outputStream)
            ExportFormat.CSV -> exportCsv(transactions, groceryBudget, outputStream)
            ExportFormat.EXCEL -> exportExcel(transactions, groceryBudget, allCategories, allSubCategories, outputStream)
        }
    }

    // =========================================================================
    // 2. IMPORT ROUTER
    // =========================================================================
    suspend fun importData(
        repository: TransactionRepository,
        format: ExportFormat,
        inputStream: InputStream
    ): ImportResult {
        return try {
            when (format) {
                ExportFormat.JSON -> importJson(repository, inputStream)
                ExportFormat.XML -> importXml(repository, inputStream)
                ExportFormat.CSV -> importCsv(repository, inputStream)
                ExportFormat.EXCEL -> importCsv(repository, inputStream) // fallback parser for tabular stream
            }
        } catch (e: Exception) {
            ImportResult(success = false, message = "Import failed: ${e.localizedMessage}")
        }
    }

    // -------------------------------------------------------------------------
    // JSON Exporter / Importer
    // -------------------------------------------------------------------------
    private fun exportJson(
        transactions: List<com.moneytracker.data.local.entity.TransactionWithCategory>,
        groceries: List<GroceryBudgetItemEntity>,
        categories: List<CategoryEntity>,
        subCategories: List<SubCategoryEntity>,
        details: List<DetailEntity>,
        unitSizes: List<UnitSizeEntity>,
        outputStream: OutputStream
    ) {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", System.currentTimeMillis())

        // Transactions
        val txnArray = JSONArray()
        transactions.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("amount", t.amount)
            obj.put("type", t.type.name)
            obj.put("categoryName", t.categoryName)
            obj.put("subCategory", t.subCategory)
            obj.put("detail", t.detail)
            obj.put("date", t.date)
            obj.put("note", t.note)
            obj.put("isRecurring", t.isRecurring)
            obj.put("recurrenceFrequency", t.recurrenceFrequency?.name ?: "ONCE_OFF")
            obj.put("recurTillDate", t.recurTillDate ?: JSONObject.NULL)
            obj.put("recurCount", t.recurCount ?: JSONObject.NULL)
            txnArray.put(obj)
        }
        root.put("transactions", txnArray)

        // Groceries
        val grocArray = JSONArray()
        groceries.forEach { g ->
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("category", g.category)
            obj.put("subCategory", g.subCategory)
            obj.put("itemDetail", g.itemDetail)
            obj.put("unitSize", g.unitSize)
            obj.put("quantityBudget", g.quantityBudget)
            obj.put("unitPriceBudget", g.unitPriceBudget)
            obj.put("costBudget", g.costBudget)
            obj.put("quantityActual", g.quantityActual)
            obj.put("unitPriceActual", g.unitPriceActual)
            obj.put("costActual", g.costActual)
            obj.put("isRecurring", g.isRecurring)
            obj.put("date", g.date)
            obj.put("note", g.note)
            grocArray.put(obj)
        }
        root.put("groceryBudget", grocArray)

        // Taxonomy
        val catArray = JSONArray()
        categories.forEach { c ->
            val obj = JSONObject()
            obj.put("name", c.name)
            obj.put("type", c.type.name)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        writer.write(root.toString(2))
        writer.flush()
    }

    private suspend fun importJson(
        repository: TransactionRepository,
        inputStream: InputStream
    ): ImportResult {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val jsonStr = reader.readText()
        val root = JSONObject(jsonStr)

        var txCount = 0
        var grocCount = 0

        // Import Categories & Taxonomy first
        val catMap = mutableMapOf<String, Long>()
        if (root.has("categories")) {
            val catArray = root.getJSONArray("categories")
            for (i in 0 until catArray.length()) {
                val obj = catArray.getJSONObject(i)
                val name = obj.getString("name")
                val type = TransactionType.valueOf(obj.getString("type"))
                val savedId = repository.saveCategory(CategoryEntity(name = name, type = type))
                catMap["${name}_${type.name}"] = savedId
            }
        }

        // Import Transactions
        if (root.has("transactions")) {
            val txnArray = root.getJSONArray("transactions")
            for (i in 0 until txnArray.length()) {
                val obj = txnArray.getJSONObject(i)
                val type = TransactionType.valueOf(obj.getString("type"))
                val catName = obj.getString("categoryName")
                val catKey = "${catName}_${type.name}"
                var catId = catMap[catKey] ?: 1L

                val entity = TransactionEntity(
                    profileId = repository.activeProfileId,
                    amount = obj.getDouble("amount"),
                    type = type,
                    categoryId = catId,
                    subCategory = obj.optString("subCategory", ""),
                    detail = obj.optString("detail", ""),
                    date = obj.optLong("date", System.currentTimeMillis()),
                    note = obj.optString("note", ""),
                    isRecurring = obj.optBoolean("isRecurring", false),
                    recurrenceFrequency = if (obj.has("recurrenceFrequency")) RecurrenceFrequency.valueOf(obj.getString("recurrenceFrequency")) else RecurrenceFrequency.ONCE_OFF,
                    recurTillDate = if (obj.isNull("recurTillDate")) null else obj.getLong("recurTillDate"),
                    recurCount = if (obj.isNull("recurCount")) null else obj.getInt("recurCount")
                )
                repository.saveTransaction(entity)
                txCount++
            }
        }

        // Import Grocery Budget
        if (root.has("groceryBudget")) {
            val grocArray = root.getJSONArray("groceryBudget")
            for (i in 0 until grocArray.length()) {
                val obj = grocArray.getJSONObject(i)
                val entity = GroceryBudgetItemEntity(
                    profileId = repository.activeProfileId,
                    category = obj.getString("category"),
                    subCategory = obj.optString("subCategory", ""),
                    itemDetail = obj.optString("itemDetail", ""),
                    unitSize = obj.optString("unitSize", ""),
                    quantityBudget = obj.optInt("quantityBudget", 1),
                    unitPriceBudget = obj.optDouble("unitPriceBudget", 0.0),
                    costBudget = obj.optDouble("costBudget", 0.0),
                    quantityActual = obj.optInt("quantityActual", 0),
                    unitPriceActual = obj.optDouble("unitPriceActual", 0.0),
                    costActual = obj.optDouble("costActual", 0.0),
                    isRecurring = obj.optInt("isRecurring", 0),
                    date = obj.optLong("date", System.currentTimeMillis()),
                    note = obj.optString("note", "")
                )
                repository.saveGroceryBudgetItem(entity)
                grocCount++
            }
        }

        return ImportResult(
            success = true,
            transactionsImported = txCount,
            groceriesImported = grocCount,
            message = "Imported $txCount transactions and $grocCount grocery items successfully."
        )
    }

    // -------------------------------------------------------------------------
    // XML Exporter / Importer
    // -------------------------------------------------------------------------
    private fun exportXml(
        transactions: List<com.moneytracker.data.local.entity.TransactionWithCategory>,
        groceries: List<GroceryBudgetItemEntity>,
        categories: List<CategoryEntity>,
        subCategories: List<SubCategoryEntity>,
        details: List<DetailEntity>,
        unitSizes: List<UnitSizeEntity>,
        outputStream: OutputStream
    ) {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<MoneyTrackerExport version=\"2\" timestamp=\"${System.currentTimeMillis()}\">\n")

        // Transactions
        sb.append("  <Transactions count=\"${transactions.size}\">\n")
        transactions.forEach { t ->
            val dateStr = Instant.ofEpochMilli(t.date).atZone(ZoneId.systemDefault()).format(dateFormatter)
            sb.append("    <Transaction>\n")
            sb.append("      <Type>${escapeXml(t.type.name)}</Type>\n")
            sb.append("      <Category>${escapeXml(t.categoryName)}</Category>\n")
            sb.append("      <SubCategory>${escapeXml(t.subCategory)}</SubCategory>\n")
            sb.append("      <Detail>${escapeXml(t.detail)}</Detail>\n")
            sb.append("      <Amount>${t.amount}</Amount>\n")
            sb.append("      <Date>${t.date}</Date>\n")
            sb.append("      <DateFormatted>${escapeXml(dateStr)}</DateFormatted>\n")
            sb.append("      <IsRecurring>${t.isRecurring}</IsRecurring>\n")
            sb.append("      <RecurrenceFrequency>${t.recurrenceFrequency?.name ?: "ONCE_OFF"}</RecurrenceFrequency>\n")
            sb.append("      <Note>${escapeXml(t.note)}</Note>\n")
            sb.append("    </Transaction>\n")
        }
        sb.append("  </Transactions>\n")

        // Groceries
        sb.append("  <GroceryBudget count=\"${groceries.size}\">\n")
        groceries.forEach { g ->
            sb.append("    <BudgetItem>\n")
            sb.append("      <Category>${escapeXml(g.category)}</Category>\n")
            sb.append("      <SubCategory>${escapeXml(g.subCategory)}</SubCategory>\n")
            sb.append("      <ItemDetail>${escapeXml(g.itemDetail)}</ItemDetail>\n")
            sb.append("      <UnitSize>${escapeXml(g.unitSize)}</UnitSize>\n")
            sb.append("      <QuantityBudget>${g.quantityBudget}</QuantityBudget>\n")
            sb.append("      <UnitPriceBudget>${g.unitPriceBudget}</UnitPriceBudget>\n")
            sb.append("      <CostBudget>${g.costBudget}</CostBudget>\n")
            sb.append("      <QuantityActual>${g.quantityActual}</QuantityActual>\n")
            sb.append("      <UnitPriceActual>${g.unitPriceActual}</UnitPriceActual>\n")
            sb.append("      <CostActual>${g.costActual}</CostActual>\n")
            sb.append("      <IsRecurring>${g.isRecurring}</IsRecurring>\n")
            sb.append("      <Date>${g.date}</Date>\n")
            sb.append("      <Note>${escapeXml(g.note)}</Note>\n")
            sb.append("    </BudgetItem>\n")
        }
        sb.append("  </GroceryBudget>\n")

        sb.append("</MoneyTrackerExport>\n")

        val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
        writer.write(sb.toString())
        writer.flush()
    }

    private suspend fun importXml(
        repository: TransactionRepository,
        inputStream: InputStream
    ): ImportResult {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val xmlContent = reader.readText()

        var txCount = 0
        var grocCount = 0

        // Parse XML Transactions
        val txnRegex = "<Transaction>([\\s\\S]*?)</Transaction>".toRegex()
        val matches = txnRegex.findAll(xmlContent)
        for (match in matches) {
            val block = match.groupValues[1]
            val typeStr = extractXmlTag(block, "Type") ?: "EXPENSE"
            val catStr = extractXmlTag(block, "Category") ?: "General"
            val subCatStr = extractXmlTag(block, "SubCategory") ?: ""
            val detailStr = extractXmlTag(block, "Detail") ?: ""
            val amountStr = extractXmlTag(block, "Amount") ?: "0.0"
            val dateStr = extractXmlTag(block, "Date") ?: "${System.currentTimeMillis()}"
            val noteStr = extractXmlTag(block, "Note") ?: ""
            val isRecStr = extractXmlTag(block, "IsRecurring") ?: "false"
            val freqStr = extractXmlTag(block, "RecurrenceFrequency") ?: "ONCE_OFF"

            val type = try { TransactionType.valueOf(typeStr) } catch (e: Exception) { TransactionType.EXPENSE }
            val catId = repository.saveCategory(CategoryEntity(name = catStr, type = type))

            repository.saveTransaction(
                TransactionEntity(
                    profileId = repository.activeProfileId,
                    amount = amountStr.toDoubleOrNull() ?: 0.0,
                    type = type,
                    categoryId = catId,
                    subCategory = subCatStr,
                    detail = detailStr,
                    date = dateStr.toLongOrNull() ?: System.currentTimeMillis(),
                    note = noteStr,
                    isRecurring = isRecStr.toBoolean(),
                    recurrenceFrequency = try { RecurrenceFrequency.valueOf(freqStr) } catch (e: Exception) { RecurrenceFrequency.ONCE_OFF }
                )
            )
            txCount++
        }

        return ImportResult(success = true, transactionsImported = txCount, groceriesImported = grocCount, message = "XML import complete: $txCount transactions.")
    }

    // -------------------------------------------------------------------------
    // CSV Exporter / Importer (RFC-4180)
    // -------------------------------------------------------------------------
    private fun exportCsv(
        transactions: List<com.moneytracker.data.local.entity.TransactionWithCategory>,
        groceries: List<GroceryBudgetItemEntity>,
        outputStream: OutputStream
    ) {
        val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)

        // Section 1: Transactions Header & Rows
        writer.write("SECTION,TRANSACTIONS\n")
        writer.write("Type,Category,SubCategory,Detail,Amount,Date,FormattedDate,IsRecurring,Frequency,Note\n")
        transactions.forEach { t ->
            val dateStr = Instant.ofEpochMilli(t.date).atZone(ZoneId.systemDefault()).format(dateFormatter)
            val line = listOf(
                escapeCsv(t.type.name),
                escapeCsv(t.categoryName),
                escapeCsv(t.subCategory),
                escapeCsv(t.detail),
                t.amount.toString(),
                t.date.toString(),
                escapeCsv(dateStr),
                t.isRecurring.toString(),
                escapeCsv(t.recurrenceFrequency?.name ?: "ONCE_OFF"),
                escapeCsv(t.note)
            ).joinToString(",")
            writer.write("$line\n")
        }

        writer.write("\nSECTION,GROCERIES\n")
        writer.write("Category,SubCategory,ItemDetail,UnitSize,QuantityBudget,UnitPriceBudget,CostBudget,QuantityActual,UnitPriceActual,CostActual,IsRecurring,Date,Note\n")
        groceries.forEach { g ->
            val line = listOf(
                escapeCsv(g.category),
                escapeCsv(g.subCategory),
                escapeCsv(g.itemDetail),
                escapeCsv(g.unitSize),
                g.quantityBudget.toString(),
                g.unitPriceBudget.toString(),
                g.costBudget.toString(),
                g.quantityActual.toString(),
                g.unitPriceActual.toString(),
                g.costActual.toString(),
                g.isRecurring.toString(),
                g.date.toString(),
                escapeCsv(g.note)
            ).joinToString(",")
            writer.write("$line\n")
        }

        writer.flush()
    }

    private suspend fun importCsv(
        repository: TransactionRepository,
        inputStream: InputStream
    ): ImportResult {
        val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        var currentSection = "TRANSACTIONS"
        var txCount = 0
        var grocCount = 0
        val lines = reader.readLines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("SECTION,")) {
                currentSection = trimmed.substringAfter("SECTION,").trim()
            } else if (trimmed.isNotBlank() && !trimmed.startsWith("Type,") && !trimmed.startsWith("Category,")) {
                val cols = parseCsvLine(trimmed)
                if (currentSection == "TRANSACTIONS" && cols.size >= 6) {
                    val type = try { TransactionType.valueOf(cols[0]) } catch (e: Exception) { TransactionType.EXPENSE }
                    val catName = cols[1]
                    val subCat = cols.getOrElse(2) { "" }
                    val detail = cols.getOrElse(3) { "" }
                    val amount = cols[4].toDoubleOrNull() ?: 0.0
                    val date = cols[5].toLongOrNull() ?: System.currentTimeMillis()
                    val note = cols.getOrElse(9) { "" }

                    val catId = repository.saveCategory(CategoryEntity(name = catName, type = type))
                    repository.saveTransaction(
                        TransactionEntity(
                            profileId = repository.activeProfileId,
                            amount = amount,
                            type = type,
                            categoryId = catId,
                            subCategory = subCat,
                            detail = detail,
                            date = date,
                            note = note
                        )
                    )
                    txCount++
                } else if (currentSection == "GROCERIES" && cols.size >= 8) {
                    val cat = cols[0]
                    val subCat = cols[1]
                    val detail = cols[2]
                    val unitSize = cols[3]
                    val qtyB = cols[4].toIntOrNull() ?: 1
                    val priceB = cols[5].toDoubleOrNull() ?: 0.0
                    val costB = cols[6].toDoubleOrNull() ?: (qtyB * priceB)
                    val qtyA = cols[7].toIntOrNull() ?: 0
                    val priceA = cols.getOrNull(8)?.toDoubleOrNull() ?: 0.0
                    val costA = cols.getOrNull(9)?.toDoubleOrNull() ?: (qtyA * priceA)

                    repository.saveGroceryBudgetItem(
                        GroceryBudgetItemEntity(
                            profileId = repository.activeProfileId,
                            category = cat,
                            subCategory = subCat,
                            itemDetail = detail,
                            unitSize = unitSize,
                            quantityBudget = qtyB,
                            unitPriceBudget = priceB,
                            costBudget = costB,
                            quantityActual = qtyA,
                            unitPriceActual = priceA,
                            costActual = costA,
                            date = System.currentTimeMillis()
                        )
                    )
                    grocCount++
                }
            }
        }

        return ImportResult(success = true, transactionsImported = txCount, groceriesImported = grocCount, message = "CSV import completed: $txCount transactions, $grocCount groceries.")
    }

    // -------------------------------------------------------------------------
    // Genuine Excel Workbook (.xlsx) OpenXML Zip Writer
    // -------------------------------------------------------------------------
    private fun exportExcel(
        transactions: List<com.moneytracker.data.local.entity.TransactionWithCategory>,
        groceries: List<GroceryBudgetItemEntity>,
        categories: List<CategoryEntity>,
        subCategories: List<SubCategoryEntity>,
        outputStream: OutputStream
    ) {
        val zip = ZipOutputStream(outputStream)

        // 1. [Content_Types].xml
        zip.putNextEntry(ZipEntry("[Content_Types].xml"))
        zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""".toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 2. _rels/.rels
        zip.putNextEntry(ZipEntry("_rels/.rels"))
        zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 3. xl/_rels/workbook.xml.rels
        zip.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 4. xl/workbook.xml
        zip.putNextEntry(ZipEntry("xl/workbook.xml"))
        zip.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
        <sheet name="Transactions" sheetId="1" r:id="rId1"/>
        <sheet name="Grocery Budget" sheetId="2" r:id="rId2"/>
    </sheets>
</workbook>""".toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 5. xl/worksheets/sheet1.xml (Transactions)
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        val sheet1Builder = StringBuilder()
        sheet1Builder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<sheetData>
<row r="1">
<c r="A1" t="inlineStr"><is><t>Type</t></is></c>
<c r="B1" t="inlineStr"><is><t>Category</t></is></c>
<c r="C1" t="inlineStr"><is><t>SubCategory</t></is></c>
<c r="D1" t="inlineStr"><is><t>Detail</t></is></c>
<c r="E1" t="inlineStr"><is><t>Amount</t></is></c>
<c r="F1" t="inlineStr"><is><t>Date</t></is></c>
<c r="G1" t="inlineStr"><is><t>Recurrence</t></is></c>
<c r="H1" t="inlineStr"><is><t>Note</t></is></c>
</row>""")
        transactions.forEachIndexed { idx, t ->
            val rowNum = idx + 2
            val dateStr = Instant.ofEpochMilli(t.date).atZone(ZoneId.systemDefault()).format(dateFormatter)
            sheet1Builder.append("""<row r="$rowNum">
<c r="A$rowNum" t="inlineStr"><is><t>${escapeXml(t.type.name)}</t></is></c>
<c r="B$rowNum" t="inlineStr"><is><t>${escapeXml(t.categoryName)}</t></is></c>
<c r="C$rowNum" t="inlineStr"><is><t>${escapeXml(t.subCategory)}</t></is></c>
<c r="D$rowNum" t="inlineStr"><is><t>${escapeXml(t.detail)}</t></is></c>
<c r="E$rowNum"><v>${t.amount}</v></c>
<c r="F$rowNum" t="inlineStr"><is><t>${escapeXml(dateStr)}</t></is></c>
<c r="G$rowNum" t="inlineStr"><is><t>${if (t.isRecurring) t.recurrenceFrequency?.name ?: "MONTHLY" else "ONCE_OFF"}</t></is></c>
<c r="H$rowNum" t="inlineStr"><is><t>${escapeXml(t.note)}</t></is></c>
</row>""")
        }
        sheet1Builder.append("</sheetData></worksheet>")
        zip.write(sheet1Builder.toString().toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        // 6. xl/worksheets/sheet2.xml (Grocery Budget)
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet2.xml"))
        val sheet2Builder = StringBuilder()
        sheet2Builder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<sheetData>
<row r="1">
<c r="A1" t="inlineStr"><is><t>Category</t></is></c>
<c r="B1" t="inlineStr"><is><t>SubCategory</t></is></c>
<c r="C1" t="inlineStr"><is><t>ItemDetail</t></is></c>
<c r="D1" t="inlineStr"><is><t>UnitSize</t></is></c>
<c r="E1" t="inlineStr"><is><t>QtyBudget</t></is></c>
<c r="F1" t="inlineStr"><is><t>UnitPriceBudget</t></is></c>
<c r="G1" t="inlineStr"><is><t>CostBudget</t></is></c>
<c r="H1" t="inlineStr"><is><t>QtyActual</t></is></c>
<c r="I1" t="inlineStr"><is><t>CostActual</t></is></c>
</row>""")
        groceries.forEachIndexed { idx, g ->
            val rowNum = idx + 2
            sheet2Builder.append("""<row r="$rowNum">
<c r="A$rowNum" t="inlineStr"><is><t>${escapeXml(g.category)}</t></is></c>
<c r="B$rowNum" t="inlineStr"><is><t>${escapeXml(g.subCategory)}</t></is></c>
<c r="C$rowNum" t="inlineStr"><is><t>${escapeXml(g.itemDetail)}</t></is></c>
<c r="D$rowNum" t="inlineStr"><is><t>${escapeXml(g.unitSize)}</t></is></c>
<c r="E$rowNum"><v>${g.quantityBudget}</v></c>
<c r="F$rowNum"><v>${g.unitPriceBudget}</v></c>
<c r="G$rowNum"><v>${g.costBudget}</v></c>
<c r="H$rowNum"><v>${g.quantityActual}</v></c>
<c r="I$rowNum"><v>${g.costActual}</v></c>
</row>""")
        }
        sheet2Builder.append("</sheetData></worksheet>")
        zip.write(sheet2Builder.toString().toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()

        zip.finish()
        zip.flush()
    }

    // Helper functions
    private fun escapeXml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun extractXmlTag(xml: String, tag: String): String? {
        val match = "<$tag>([\\s\\S]*?)</$tag>".toRegex().find(xml)
        return match?.groupValues?.get(1)?.trim()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }
}
