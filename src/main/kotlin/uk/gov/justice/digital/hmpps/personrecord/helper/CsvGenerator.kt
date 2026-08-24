package uk.gov.justice.digital.hmpps.personrecord.helper

import java.io.FileWriter
import java.sql.Connection
import java.sql.DriverManager

object CsvGenerator {
  val queries = listOf(
    "SELECT prison_number from personrecordservice.person where source_system = 'NOMIS'",
    "SELECT crn from personrecordservice.person where source_system = 'DELIUS'",
    "SELECT defendant_id from personrecordservice.person where source_system = 'COMMON_PLATFORM'",
  )

  // Address lookups are keyed by crn + cprAddressId together (update_id belongs to a specific person),
  // so this is queried as a joined pair rather than independent columns like the queries above.
  const val CRN_ADDRESS_QUERY =
    "SELECT p.crn, a.update_id FROM personrecordservice.person p " +
      "JOIN personrecordservice.address a ON a.fk_person_id = p.id " +
      "WHERE p.source_system = 'DELIUS' AND a.update_id IS NOT NULL"

  @JvmStatic
  fun main(args: Array<String>) {
    val (url, user, pass, outFile) = args
    DriverManager.getConnection(url, user, pass).use { conn ->
      val columns = queries.map { runQuery(conn, it) }
      val crnAddressPairs = runPairQuery(conn, CRN_ADDRESS_QUERY)
      val rowCount = minOf(columns.minOf { it.size }, crnAddressPairs.size)
      val rows = (0 until rowCount).map { i ->
        listOf(columns[0][i], columns[1][i], columns[2][i], crnAddressPairs[i].first, crnAddressPairs[i].second)
      }
      writeCsv(rows, outFile)
    }
    println("Done -> $outFile")
  }

  fun runQuery(conn: Connection, sql: String): List<String> = conn.createStatement().use { st ->
    st.executeQuery(sql).use { rs ->
      val result = mutableListOf<String>()
      while (rs.next()) {
        result += rs.getString(1) ?: ""
      }
      result
    }
  }

  fun runPairQuery(conn: Connection, sql: String): List<Pair<String, String>> = conn.createStatement().use { st ->
    st.executeQuery(sql).use { rs ->
      val result = mutableListOf<Pair<String, String>>()
      while (rs.next()) {
        result += (rs.getString(1) ?: "") to (rs.getString(2) ?: "")
      }
      result
    }
  }

  fun writeCsv(rows: List<List<String>>, file: String) {
    FileWriter(file).use { w ->
      w.append("prison_number,crn,defendant_id,address_crn,cpr_address_id\n")
      rows.forEach { w.append(it.joinToString(",")).append("\n") }
    }
  }
}
