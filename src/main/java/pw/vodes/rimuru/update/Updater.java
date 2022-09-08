package pw.vodes.rimuru.update;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.Main;

public class Updater {
	
	private File jar;
	
	public Updater init() {
		String path = Updater.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		try {
			String decodedPath = URLDecoder.decode(path, "UTF-8");
			jar = new File(decodedPath);
		} catch (UnsupportedEncodingException e) {
			jar = new File(path);
		}
		return this;
	}
	
	public boolean run(MessageCreateEvent event) {
		try {
			var repoDir = new File(Main.getFiles().getConfigDir(), "Rimuru-Git");
			var git = Git.cloneRepository()
					.setURI(Main.getConfigFile().updateConfig.git_repo)
					.setBranch(Main.getConfigFile().updateConfig.branch)
					.setDirectory(repoDir)
					.call();
			
			var logs = git.log().call();
			for(var log : logs) {
				System.out.println(log.getName());
				break;
			}
			//FileUtils.deleteQuietly(repoDir);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public void restart() {
		
	}
}
