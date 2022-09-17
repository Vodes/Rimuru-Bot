package pw.vodes.rimuru.update;

public class UpdateConfig {
	
	public String git_repo = "https://github.com/Vodes/Rimuru-Bot.git";
	public String branch = "master";
	public String current_commit = "";
	
	public String custom_jvm_args = "-XX:+UnlockExperimentalVMOptions -XX:+UseZGC -Xms1G -Xmx2G";
	public boolean allow_update = true;
	public String restart_trigger = "";
	
}
	