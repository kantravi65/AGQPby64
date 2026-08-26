import java.time.LocalDate
import java.time.Period
import kotlin.random.Random

fun main() {
    val startDate = LocalDate.of(2008, 12, 31)
    val currentDate = LocalDate.now()
    val period = Period.between(startDate, currentDate)
    val years = period.years
    val months = period.months
    val days = period.days
    val randomNum = Random.nextInt(10, 100)
    
    val formattedDate = String.format("%02d%02d%02d", years, months, days)
    val code = "RYQP-$formattedDate-$randomNum"
    println(code)
}
