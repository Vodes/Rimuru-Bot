package pw.vodes.rimuru.projects;

import java.util.ArrayList;

import com.fasterxml.jackson.core.JsonProcessingException;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.audit.CommandStaffAction;
import pw.vodes.rimuru.projects.types.Project;

public class Projects {
	
	private static ArrayList<Project> projects = new ArrayList<Project>();
	
	public static void addProject(Project pro) {
		projects.add(pro);
		save();
	}
	
	public static void editProject(int index, Project pro) {
		projects.set(index, pro);
		save();
	}
	
	public static void removeProject(Project pro) {
		projects.remove(pro);
		save();
	}
	
	@SuppressWarnings("unchecked")
	public static void load() {
		if (Main.getFiles().exists("projects.json") && !Main.getFiles().read("projects.json").isBlank()) {
			try {
				projects = (ArrayList<Project>) Main.getMapper().readValue(Main.getFiles().read("projects.json"),
						Main.getMapper().getTypeFactory().constructCollectionType(ArrayList.class, Project.class));
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static void save() {
		try {
			Main.getFiles().write("projects.json", Main.getMapper().writeValueAsString(projects));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList<Project> getProjects() {
		return projects;
	}

}
