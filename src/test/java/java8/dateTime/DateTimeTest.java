package java8.dateTime;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;

import org.junit.Test;

public class DateTimeTest {
	@Test 
	public void localDateTimeCreate() {
		LocalDateTime timePoint = LocalDateTime.now();     // The current date and time
		
		LocalDate datePoint = LocalDate.of(2012, Month.DECEMBER, 12); // from values
		
		System.out.println(datePoint.toString());
		
		
		LocalDate.ofEpochDay(150);  // middle of 1970
		LocalTime theTime = LocalTime.of(17, 18); // the train I took home today
		
		System.out.println(theTime);
		LocalTime.parse("10:15:30"); // From a String
		
	}
}
