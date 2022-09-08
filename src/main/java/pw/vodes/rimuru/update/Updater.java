package pw.vodes.rimuru.update;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.javacord.api.event.message.MessageCreateEvent;

import pw.vodes.rimuru.Main;
import pw.vodes.rimuru.Util;

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
		var repoDir = new File(Main.getFiles().getConfigDir(), "Rimuru-Git");
		Git git = null;
		try {
			var msg = event.getChannel().sendMessage("Cloning repo...").get();
			git = Git.cloneRepository()
					.setURI(Main.getConfigFile().updateConfig.git_repo)
					.setBranch(Main.getConfigFile().updateConfig.branch)
					.setDirectory(repoDir)
					.call();
			
			var logs = git.log().call();
			String newest = "";
			for(var log : logs) {
				newest = log.getName();
				break;
			}
			
			msg.edit("Repo cloned. Newest commit: `" + newest + "`");
			
			var progressString = "Building jar from newest commit...";
			msg = event.getChannel().sendMessage(progressString).get();
			var process = Util.getSystemProcessBuilder(Arrays.asList("mvn clean compile assembly:single")).directory(repoDir).start();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line = null;
				while ((line = reader.readLine()) != null) {
					if(line.trim().equalsIgnoreCase("[INFO] BUILD SUCCESS"))
						msg.edit(progressString = (progressString + " Done ✅"));
					
					if(StringUtils.startsWithIgnoreCase(line.trim(), "[INFO] Total time:")) {
						msg.edit(progressString + "\nBuild took: " + line.split(":")[1].trim());
					}
				}
			} catch (Exception e) {
				Util.reportException(e, this.getClass().getCanonicalName() + " at mvn process");
			}
			var targetDir = new File(repoDir, "target");
			File newJar = null;
			for(var file : targetDir.listFiles()) {
				if(file.getName().toLowerCase().contains(".jar")) {
					newJar = file;
					break;
				}
			}
			if(newJar != null) {
				if(this.jar.getName().toLowerCase().contains(".jar")) {
					Files.move(newJar.toPath(), jar.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
				event.getChannel().sendMessage("Moved finished build.\nRestarting...");
			} else {
				return false;
			}
			
			Main.getConfigFile().updateConfig.current_commit = newest;
			Main.getConfigFile().updateConfig.restart_trigger = event.getMessageLink().toString();
			Main.getFiles().saveConfig();
			return true;
		} catch (Exception e) {
			Util.reportException(e, this.getClass().getCanonicalName());
		} finally {
			if(git != null)
				git.close();
			FileUtils.deleteQuietly(repoDir);
		}
		return false;
	}
	
	public void restart() {

	}
}
