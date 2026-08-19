package uk.gov.justice.digital.hmpps.personrecord.helper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

class CsvGeneratorTest {

  @Test
  fun `writeCsv writes a header row followed by comma separated data rows`() {
    val file = File.createTempFile("csv-generator-test", ".csv")
    file.deleteOnExit()

    CsvGenerator.writeCsv(
      rows = listOf(
        listOf("G9482GV", "X988723", "f89090fb-5189-4879-bf16-e022e1599ed8"),
        listOf("A4409EC", "X991095", "d34fa6bf-0634-47f6-bd1b-37f551ae5da4"),
      ),
      file = file.absolutePath,
    )

    val lines = file.readLines()
    assertThat(lines).containsExactly(
      "prison_number,crn,defendant_id",
      "G9482GV,X988723,f89090fb-5189-4879-bf16-e022e1599ed8",
      "A4409EC,X991095,d34fa6bf-0634-47f6-bd1b-37f551ae5da4",
    )
  }

  @Test
  fun `writeCsv writes only the header row when there are no data rows`() {
    val file = File.createTempFile("csv-generator-test", ".csv")
    file.deleteOnExit()

    CsvGenerator.writeCsv(rows = emptyList(), file = file.absolutePath)

    assertThat(file.readLines()).containsExactly("prison_number,crn,defendant_id")
  }

  @Test
  fun `runQuery returns each value from the result set's first column in order`() {
    val connection = mock<Connection>()
    val statement = mock<Statement>()
    val resultSet = mock<ResultSet>()

    whenever(connection.createStatement()).thenReturn(statement)
    whenever(statement.executeQuery("SELECT prison_number")).thenReturn(resultSet)
    whenever(resultSet.next()).thenReturn(true, true, false)
    whenever(resultSet.getString(1)).thenReturn("G9482GV", "A4409EC")

    val result = CsvGenerator.runQuery(connection, "SELECT prison_number")

    assertThat(result).containsExactly("G9482GV", "A4409EC")
  }

  @Test
  fun `runQuery substitutes an empty string for null column values`() {
    val connection = mock<Connection>()
    val statement = mock<Statement>()
    val resultSet = mock<ResultSet>()

    whenever(connection.createStatement()).thenReturn(statement)
    whenever(statement.executeQuery("SELECT crn")).thenReturn(resultSet)
    whenever(resultSet.next()).thenReturn(true, false)
    whenever(resultSet.getString(1)).thenReturn(null)

    val result = CsvGenerator.runQuery(connection, "SELECT crn")

    assertThat(result).containsExactly("")
  }
}
