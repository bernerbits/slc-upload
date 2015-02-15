package net.bernerbits.avolve.slcupload.state;

public interface TaskHandler<TD extends TaskDescriptor> {

	public void handle(TD task);
	
}
