package pw.vodes.rimuru.projects.types;

import java.util.ArrayList;

public class Project {
	
	public String name;
	public ArrayList<String> aliases = new ArrayList<String>();
	public ArrayList<Episode> episodes = new ArrayList<Episode>();
	
	public String releasePingRole;
	
	// Project Specific User-Management
	public String leaderID;
	public ArrayList<String> adminIDs = new ArrayList<String>();
	public ArrayList<String> memberIDs = new ArrayList<String>();
	
}
