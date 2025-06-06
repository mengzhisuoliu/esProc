package com.scudata.expression.fn.algebra;

import com.scudata.common.Logger;
import com.scudata.common.MessageManager;
import com.scudata.common.RQException;
import com.scudata.dm.Context;
import com.scudata.dm.Sequence;
import com.scudata.expression.Function;
import com.scudata.expression.IParam;
import com.scudata.expression.ParamParser;
import com.scudata.resources.EngineMessage;

/**
 * 处理函数polyfit(A,B,n)，用n次多项式拟合向量X和Y
 * 通过解法方程获得多项式系数
 * @author bd
 */
public class Polyfit extends Function {
	/**
	 * 检查表达式的有效性，无效则抛出异常
	 */
	public void checkValidity() {
		if (param == null) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("polyfit" + mm.getMessage("function.missingParam"));
		}
	}

	public Object calculate(Context ctx) {
		if (param.isLeaf()) {
			MessageManager mm = EngineMessage.get();
			throw new RQException("polyfit" + mm.getMessage("function.invalidParam"));
		} else {
			if (param.getSubSize() != 3) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("polyfit" + mm.getMessage("function.invalidParam"));
			}

			IParam sub1 = param.getSub(0);
			IParam sub2 = param.getSub(1);
			IParam sub3 = param.getSub(2);
			if (sub1 == null || sub2 == null || sub3 == null) {
				MessageManager mm = EngineMessage.get();
				throw new RQException("polyfit" + mm.getMessage("function.invalidParam"));
			}
			
			Object o1 = sub1.getLeafExpression().calculate(ctx);
			Object o2 = sub2.getLeafExpression().calculate(ctx);
			Object o3 = sub3.getLeafExpression().calculate(ctx);
			if (o1 instanceof Sequence && o2 instanceof Sequence && o3 instanceof Number) {
				int n = ((Number) o3).intValue();
				if (n < 1) {
					n = 1;
				} 
				Sequence A = (Sequence) o1;
				Sequence B = (Sequence) o2;
				int size = A.length();
				if (size != B.length()) {
					// 不同维
					MessageManager mm = EngineMessage.get();
					Logger.warn("polyfit" + mm.getMessage("function.paramTypeError"));
					return null;
				}
				double[] as = new double[size];
				double[] bs = new double[size];
				for (int i = 1; i <= size; i++) {
					Object o = A.get(i);
					if (o instanceof Number) {
						as[i-1] = ((Number) o).doubleValue();
					}
					o = B.get(i);
					if (o instanceof Number) {
						bs[i-1] = ((Number) o).doubleValue();
					}
				}
				
				Matrix P = new Matrix(n+1, n+1);
				double[][] ps = P.getArray();
				Matrix Y = new Matrix(n+1, 1);
				double[][] ys = Y.getArray();
				for (int i = 0; i < n+1; i++) {
					for (int j = 0; j < n+1; j++) {
						if (i > j) {
							// 已经计算过的
							ps[i][j] = ps[j][i];
						}
						else {
							// 对角线上及右上三角的系数
							double p = 0;
							for (int k = 0; k < size; k++) {
								p += Math.pow(as[k], i+j);
							}
							ps[i][j] = p;
						}
					}
					double p = 0;
					for (int k = 0; k < size; k++) {
						p += Math.pow(as[k], i) * bs[k];
					}
					ys[i][0] = p;
				}
				Matrix X = P.solve(Y);
				if (X == null) {
					return null;
				}
				//return X.toSequence(option, false);
				//edited by bd, 2025.4.23, 返回值不和matlab统一了，返回序列
				double[][] vs = X.getArray();
				int rows = vs.length;
				if (rows > 0) {
					int cols = vs[0].length;
					if (cols == 1) {
						Sequence result = new Sequence(rows);
						for (int i = 0; i < rows; i++) {
							result.add(Linefit.getValue(vs[i][0]));
						}
						return result;
					}
					else if (rows == 1) {
						Sequence result = new Sequence(cols);
						for (int i = 0; i < cols; i++) {
							result.add(Linefit.getValue(vs[0][i]));
						}
						return result;
					}
				}
				return null;
			} else {
				MessageManager mm = EngineMessage.get();
				throw new RQException("polyfit" + mm.getMessage("function.paramTypeError"));
			}
		}
	}
	
    public static void main(String[] args) {
    	Polyfit func = new Polyfit();
        double[] h_A = {3,5,2,8};
        double[] h_B = {4,6,2,4};
        Sequence A = Linefit.toSeq(h_A, h_A.length);
        Sequence B = Linefit.toSeq(h_B, h_B.length);
		Context ctx = new Context();
        ctx.setParamValue("A", A);
        ctx.setParamValue("B", B);
        func.option = "1";

    	IParam params = ParamParser.parse("A,B,2", null, ctx);
        func.param = params;
        Object res = func.calculate(ctx);
        Linefit.print(((Sequence)res), 10);
    }
}
