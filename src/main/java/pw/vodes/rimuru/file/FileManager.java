package pw.vodes.rimuru.file;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

public class FileManager {
	
	private String configDir = null;
	
	public FileManager(String configDir) {
		this.configDir = configDir;
	} 
	
	
	public String read(String name) {
		if(exists(name)) {
			try {
				return FileUtils.readFileToString(new File(getConfigDir(), name), StandardCharsets.UTF_8);
			} catch (IOException e) {}
		}
		return "";
	}
	
	public boolean write(String name, String data) {
		return write(name, data, false);
	}
	
	public boolean write(String name, String data, boolean append) {
		try {
			FileUtils.write(new File(getConfigDir(), name), data, StandardCharsets.UTF_8, append);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	public boolean exists(String name) {
		return new File(getConfigDir(), name).exists();
	}
	
	public File getConfigDir() {
		File file = null;
		
		if(configDir != null) {
			file = new File(configDir);
		} else {
			var homeDir = new File(System.getProperty("user.home"));
			if(SystemUtils.IS_OS_LINUX) {
				file = new File(homeDir, ".config" + File.separator + "Rimuru");
			} else {
				file = new File(homeDir, "AppData" + File.separator + "Roaming" + File.separator + "Rimuru");
			}
		}

		if(!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

}
