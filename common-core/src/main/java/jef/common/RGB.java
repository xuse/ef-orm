/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jef.common;

import java.awt.Color;
import java.util.Random;

import jef.tools.Assert;
import jef.tools.StringUtils;

/**
 * 用于描述RGB色彩空间的一个颜色
 * @author Administrator
 */
public class RGB {
	public int red;

	public int green;

	public int blue;

	public RGB() {
	};

	public RGB(int r, int g, int b) {
		this.red = r;
		this.green = g;
		this.blue = b;
	}

	public int getBlue() {
		return blue;
	}

	public int getGreen() {
		return green;
	}

	public int getRed() {
		return red;
	}

	public void setData(int[] data) {
		Assert.equals(data.length, 3, "A array to describe cloor must be length=3");
		red = data[0];
		green = data[1];
		blue = data[2];
	}

	public static RGB getInstance(Color color) {
		return getInstance(color.getRGB());
	}
	
	public static RGB getInstance(int[] data) {
		RGB rgb = new RGB();
		rgb.setData(data);
		return rgb;
	}

	public static RGB getInstance(int data) {
		RGB rgb = new RGB();
		rgb.blue = (data & 0xff);
		rgb.green = (data >> 8 & 0xff);
		rgb.red = (data >> 16 & 0xff);
		return rgb;

	}

	public int[] getData() {
		return new int[] { red, green, blue };
	}

	public String toString() {
		return StringUtils.join(getData(), ",");
	}

	public int distanceGray() {
		int min = Math.min(Math.min(red, green), blue);
		int ro = red - min;
		int go = green - min;
		int bo = blue - min;
		return Math.max(Math.max(ro, go), bo);
	}

	/**
	 * yuv色彩模型来源于rgb模型， 该模型的特点是将亮度和色度分离开，从而适合于图像处理领域。 basic color model used in
	 * analogue color TV broadcasting.
	 * 
	 * @return
	 */
	public static RGB fromYUV(float[] yuv) {
		RGB rgb = new RGB();
		rgb.red = (int) (yuv[0] + 1.14 * yuv[2] + .5f);
		rgb.green = (int) (yuv[0] - 0.39 * yuv[1] - 0.58 * yuv[2] + .5f);
		rgb.blue = (int) (yuv[0] + 2.03 * yuv[1] + .5f);
		return rgb;
	}

	/**
	 * 转换到YUV空间
	 * 
	 * @return
	 */
	public float[] toYUV() {
		float yuv[] = new float[3];
		yuv[0] = (0.299f * red + 0.587f * green + 0.114f * blue);
		yuv[1] = (-0.147f * red - 0.289f * green + 0.436f * blue);
		yuv[2] = (0.615f * red - 0.515f * green - 0.100f * blue);
		return yuv;
	}

	/**
	 * YCbCr模型来源于yuv模型。YCbCr is a scaled and offset version of the YUV color
	 * space. 应用：数字视频
	 * 
	 * @return
	 */
	public float[] toYCrCb() {
		float yuv[] = new float[3];
		yuv[0] = 0.299f * red + 0.587f * green + 0.114f * blue;
		yuv[1] = 0.5f * red - 0.4187f * green - 0.0813f * blue + 128;
		yuv[2] = -0.1687f * red - 0.3313f * green + 0.5f * blue + 128;
		return yuv;

	}

	/**
	 * 转换到YCrCb空间
	 * 
	 * @param yCrCb
	 * @return
	 */
	public static RGB fromYCrCb(float[] yCrCb) {
		int R = (int) (yCrCb[0] + 1.402 * (yCrCb[1] - 128) + .5f);
		int G = (int) (yCrCb[0] - (0.34414 * (yCrCb[2] - 128)) - (0.71414 * (yCrCb[1] - 128)) + .5f);
		int B = (int) (yCrCb[0] + (1.772 * (yCrCb[2] - 128)) + .5f);
		return new RGB(R, G, B);
	}

	/**
	 * 从HSB空间转回
	 */
	public static RGB fromHSB(float[] fs) {
		return RGB.getInstance(Color.HSBtoRGB(fs[0], fs[1], fs[2]));
	}

	/**
	 * 转换到HSB空间(圆锥空间) 返回：弧度、0~1,0~1比例。
	 * 一个参数用弧度表示在锥形地面上的角度——即色相（红色为0度，Green是120度，Blue在240度，对应为弧度red=0,green=2/3*PI,Blue是4/3*PI）
	 * 
	 * 
	 */
	public float[] toHSB() {
		float[] hsb = Color.RGBtoHSB(red, green, blue, null);
		return hsb;
	}

	/**
	 * 转换到HSL空间(圆锥空间) 返回：0~1,0~1,0~1比例
	 * 
	 * 
	 * HSL即色相、饱和度、亮度（英语：Hue, Saturation, Lightness），又称HLS。HSV即色相、饱和度、明度（英语：Hue, Saturation, Value），又称HSB，其中B即英语：Brightness。
	 */
	public float[] toHSL() {
		float H, S, L, var_Min, var_Max, del_Max, del_R, del_G, del_B;
		H = 0;
		var_Min = Math.min(red, Math.min(blue, green));
		var_Max = Math.max(red, Math.max(blue, green));
		del_Max = var_Max - var_Min;
		L = (var_Max + var_Min) / 2;
		if (del_Max == 0) {// 灰度色
			H = 0;
			S = 0;
		} else {
			if (L < 128) {
				S = 255 * del_Max / (var_Max + var_Min);
			} else {
				S = 255 * del_Max / (512 - var_Max - var_Min);
			}
			del_R = ((360 * (var_Max - red) / 6) + (360 * del_Max / 2)) / del_Max;
			del_G = ((360 * (var_Max - green) / 6) + (360 * del_Max / 2)) / del_Max;
			del_B = ((360 * (var_Max - blue) / 6) + (360 * del_Max / 2)) / del_Max;
			if (red == var_Max) {
				H = del_B - del_G;
			} else if (green == var_Max) {
				H = 120 + del_R - del_B;
			} else if (blue == var_Max) {
				H = 240 + del_G - del_R;
			}
			if (H < 0)
				H += 360;
			if (H >= 360)
				H -= 360;
		}
		return new float[] { H / 360, S / 255, L / 255 };
	}

	/**
	 * 从HSL空间转换
	 */
	public static RGB fromHSL(float[] hsl) {
		if (hsl == null) {
			return null;
		}
		float H = hsl[0] * 360;
		float S = hsl[1] * 255;
		float L = hsl[2] * 255;
		float R, G, B, var_1, var_2;
		if (S == 0) {
			R = L;
			G = L;
			B = L;
		} else {
			if (L < 128) {
				var_2 = (L * (256 + S)) / 256;
			} else {
				var_2 = (L + S) - (S * L) / 256;
			}

			if (var_2 > 255) {
				var_2 = Math.round(var_2);
			}

			if (var_2 > 254) {
				var_2 = 255;
			}

			var_1 = 2 * L - var_2;
			R = RGBFromHue(var_1, var_2, H + 120);
			G = RGBFromHue(var_1, var_2, H);
			B = RGBFromHue(var_1, var_2, H - 120);
		}
		R = R < 0 ? 0 : R;
		R = R > 255 ? 255 : R;
		G = G < 0 ? 0 : G;
		G = G > 255 ? 255 : G;
		B = B < 0 ? 0 : B;
		B = B > 255 ? 255 : B;
		return new RGB((int) Math.round(R), (int) Math.round(G), (int) Math.round(B));
	}

	
	public static float RGBFromHue(float a, float b, float h) {
		if (h < 0) {
			h += 360;
		}
		if (h >= 360) {
			h -= 360;
		}
		if (h < 60) {
			return a + ((b - a) * h) / 60;
		}
		if (h < 180) {
			return b;
		}

		if (h < 240) {
			return a + ((b - a) * (240 - h)) / 60;
		}
		return a;
	}

	/**
	 * 指定在角度和长度上的缩放比例后返回HSL颜色值
	 * Photoshop中的HSL就是 toHSL(360,100)
	 * Windows绘图中的HSL就是 toHSL(240,240)
	 * @param i
	 * @param j
	 * @return
	 */
	public int[] toHSL(int i, int j) {
		float[] hsl = toHSL();
		int[] data = new int[3];
		data[0] = (int) (hsl[0] * i + 0.5f);
		data[1] = (int) (hsl[1] * j + 0.5f);
		data[2] = (int) (hsl[2] * j + 0.5f);
		return data;
	}

	/**
	 * 指定在角度和长度上的缩放比例后返回HSB颜色值(某些绘图软件中称为HSV)<br>
	 * HSL和HSB(HSV)都是锥形色彩空间，因此其
	 * 
	 * Photoshop中的HSV就是 toHSB(360,100)
	 * Windows绘图中的HSV就是 toHSB(240,240)
	 * @param i
	 * @param j
	 * @return
	 */
	public int[] toHSB(int i, int j) {
		float[] hsb = toHSB();
		int[] data = new int[3];
		data[0] = (int) (hsb[0] * i + 0.5f);
		data[1] = (int) (hsb[1] * j + 0.5f);
		data[2] = (int) (hsb[2] * j + 0.5f);
		return data;
	}
	
	/**
	 * 转换为java的Color对象
	 * @return
	 */
	public Color toColor(){
		return new Color(red,green,blue);
	}

	/**
	 * 产生一个随机颜色
	 * @return
	 */
	public static RGB randomColor() {
		Random r=new Random();
		int a=((int)r.nextInt(52))*5;
		int b=((int)r.nextInt(52))*5;
		int c=((int)r.nextInt(52))*5;
		return new RGB(a,b,c);
	}
}
