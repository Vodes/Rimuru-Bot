package pw.vodes.rimuru.projects.types;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Episode {
	
	public String episode;
	public long airTime;
	public boolean released;
	public String nyaaLink;
	public ArrayList<Position> positions = new ArrayList<Position>();
	
	@JsonIgnore
	public boolean allPositionsDone() {
		for(var pos : positions) {
			if(!pos.done)
				return false;
		}
		return true;
	}

}
