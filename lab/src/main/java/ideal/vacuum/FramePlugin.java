package ideal.vacuum;

public interface FramePlugin{

	void refresh();
	
	void anim(float angleRotation, float xTranslation);

	void close() ;

	void display() ;
}
