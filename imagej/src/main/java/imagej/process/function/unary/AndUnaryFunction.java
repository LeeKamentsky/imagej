package imagej.process.function.unary;

import imagej.process.TypeManager;
import mpicbg.imglib.type.numeric.RealType;

public class AndUnaryFunction implements UnaryFunction
{
	private RealType<?> targetType;
	private double constant;
	
	public AndUnaryFunction(RealType<?> targetType, double constant)
	{
		this.targetType = targetType;
		this.constant = constant;
	}
	
	public double compute(double input)
	{
		double value = ((long)input) & ((long)constant);
			
		value = TypeManager.boundValueToType(targetType, value);

		return value;
	}
}

