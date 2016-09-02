package ideal.vacuum.ernest;


import ideal.vacuum.tracing.ITracer;
import spacegraph.math.Matrix3f;
import spacegraph.math.Quat4f;
import spacegraph.math.v3;
import spacegraph.phys.math.Transform;


public class EffectImpl implements Effect
{
	private String m_label = "";
	private final v3 m_location = new v3();
	private final Transform m_transformation = new Transform();
	private int m_color = 0xFFFFFF;
	private float m_angle;
	private String enactedInteractionLabel;
	
	@Override
    public void setLabel(String label)
	{
		m_label = label;
	}

	@Override
    public String getLabel()
	{
		return m_label;
	}

	@Override
    public void setEnactedInteractionLabel(String label)
	{
		this.enactedInteractionLabel = label;
	}

	@Override
    public String getEnactedInteractionLabel()
	{
		return this.enactedInteractionLabel;
	}

	@Override
    public void setLocation(v3 location)
	{
		m_location.set(location);
	}

	@Override
    public v3 getLocation()
	{
		return m_location;
	}

//	public void setTransformation(Transform transformation)
//	{
//		m_transformation.set(transformation);
//	}

	@Override
    public void setTransformation(float angle, float x)
	{
		m_transformation.setIdentity();
		m_angle = angle;
		m_transformation.setRotation(Quat4f.angle(0,0,1,angle));
		m_transformation.set(x,0,0);
	}
	
	@Override
    public Transform getTransformation()
	{
		return m_transformation;
	}

	@Override
    public void setColor(int color)
	{
		m_color = color;
	}

	@Override
    public int getColor()
	{
		return m_color;
	}
	
	@Override
    public void trace(ITracer tracer)
	{
		if (tracer != null)
		{
			Object e = tracer.addEventElement("effect");
			tracer.addSubelement(e, "label", m_label + "");
			tracer.addSubelement(e, "color", ErnestUtils.hexColor(m_color));
			tracer.addSubelement(e, "position_x", ErnestUtils.format(m_location.x,0));
			tracer.addSubelement(e, "position_y", ErnestUtils.format(m_location.y,0));
			Matrix3f rot = new Matrix3f();
			v3 trans = new v3();
//			m_transformation.get(rot, trans);
//			tracer.addSubelement(e, "translation_x", ErnestUtils.format(trans.x,0));
//			tracer.addSubelement(e, "translation_y", ErnestUtils.format(trans.y,0));
//			//tracer.addSubelement(e, "rotation", m_angle +"");
//			tracer.addSubelement(e, "rotation", ErnestUtils.format((float) - Math.atan2(rot.m01,rot.m00),2));
		}
	}
}
