package stream.api;

import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Random;

public class SeedToDo {
	public static void main(String [] args) throws Exception{
		String [] categories = {
				"Provide profile", 
				"Provide updated profile",
				"Provide copy of issued licenses",
				"Provide data room documents",
				"Other"
		};
		
		/*
		for(String category : categories) {
			try {
				ToDoCategory cat = new ToDoCategory();
				cat.toDoCategory=category;
				SeedData.saveAndGetId(cat, "toDoCategory");
			}
			catch(Exception e) {
				
			}
		}
		
		*/
		int companyId = SeedData.queryForId("name=Cord", "companies");
		LocalDateTime timePoint = LocalDateTime.now(); 
		Random ran = new Random();

		for(String category : categories) {
			try {
				int categoryId = SeedData.queryForId("toDoCategory=" + URLEncoder.encode(category), "toDoCategory");
				
				ToDo t = new ToDo(companyId, categoryId);
				
				for(int i=0;i<10;++i) {
					t.comments = "Random Comment " + Math.random();
					
					LocalDateTime d = timePoint.plusDays(ran.nextInt(90));
					ZonedDateTime zdt = d.atZone(ZoneId.systemDefault());
					t.dueDate = Date.from(zdt.toInstant());
					t.status="N";
					
					SeedData.saveAndGetId(t, "todos");
				}
			}
			catch(Exception e) {
				
			}
		}

	}
}
