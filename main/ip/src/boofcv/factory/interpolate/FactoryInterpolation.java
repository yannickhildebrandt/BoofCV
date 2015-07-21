/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.factory.interpolate;

import boofcv.abst.filter.interpolate.InterpolatePixel_S_to_MB_MultiSpectral;
import boofcv.alg.interpolate.InterpolatePixelMB;
import boofcv.alg.interpolate.InterpolatePixelS;
import boofcv.alg.interpolate.InterpolateRectangle;
import boofcv.alg.interpolate.TypeInterpolate;
import boofcv.alg.interpolate.impl.*;
import boofcv.alg.interpolate.kernel.BicubicKernel_F32;
import boofcv.core.image.border.BorderType;
import boofcv.core.image.border.FactoryImageBorder;
import boofcv.struct.image.*;

/**
 * Simplified interface for creating interpolation classes.
 *
 * @author Peter Abeles
 */
@SuppressWarnings({"unchecked"})
public class FactoryInterpolation {

	/**
	 * Returns {@link InterpolatePixelS} of the specified type.
	 *
	 * @param min Minimum possible pixel value.  Inclusive.
	 * @param max Maximum possible pixel value.  Inclusive.
	 * @param type Type of interpolation.
	 * @param dataType Type of gray-scale image
	 * @return Interpolation for single band image
	 */
	public static <T extends ImageSingleBand> InterpolatePixelS<T>
	createPixelS(double min, double max, TypeInterpolate type, BorderType borderType , ImageDataType dataType )
	{

		Class t = ImageDataType.typeToSingleClass(dataType);

		return createPixelS(min,max,type,borderType,t);
	}

	/**
	 * Creates an interpolation class of the specified type for the specified image type.
	 *
	 * @param min Minimum possible pixel value.  Inclusive.
	 * @param max Maximum possible pixel value.  Inclusive.
	 * @param type Interpolation type
	 * @param borderType Border type. If null then it will not be set here.
	 * @param imageType Type of input image
	 * @return Interpolation
	 */
	public static <T extends ImageSingleBand> InterpolatePixelS<T>
	createPixelS(double min, double max, TypeInterpolate type, BorderType borderType, Class<T> imageType)
	{
		InterpolatePixelS<T> alg;

		switch( type ) {
			case NEAREST_NEIGHBOR:
				alg = nearestNeighborPixelS(imageType);
				break;

			case BILINEAR:
				return bilinearPixelS(imageType, borderType);

			case BICUBIC:
				alg = bicubicS(0.5f, (float) min, (float) max, imageType);
				break;

			case POLYNOMIAL4:
				alg = polynomialS(4, min, max, imageType);
				break;

			default:
				throw new IllegalArgumentException("Add type: "+type);
		}

		if( borderType != null )
			alg.setBorder(FactoryImageBorder.general(imageType, borderType));
		return alg;
	}

	/**
	 * Pixel based interpolation on multi-band image
	 *
	 * @param min Minimum possible pixel value.  Inclusive.
	 * @param max Maximum possible pixel value.  Inclusive.
	 * @param type Interpolation type
	 * @param imageType Type of input image
	 */
	public static <T extends ImageMultiBand> InterpolatePixelMB<T>
	createPixelMB(double min, double max, TypeInterpolate type, BorderType borderType, ImageType<T> imageType )
	{
		switch (imageType.getFamily()) {

			case MULTI_SPECTRAL:
				return (InterpolatePixelMB)createPixelMB(createPixelS(min,max,type,borderType,imageType.getDataType()));

			case SINGLE_BAND:
				throw new IllegalArgumentException("Need to specify a multi-band image type");

			case INTERLEAVED:
				throw new IllegalArgumentException("Not yet supported.  Post a message letting us know you need this." +
						"  Use MultiSpectral instead for now.");

			default:
				throw new IllegalArgumentException("Add type: "+type);
		}
	}

	/**
	 * Converts a single band interpolation algorithm into a mult-band interpolation for {@link MultiSpectral} images.
	 * NOTE: If a specialized interpolation exists you should use that instead of this the specialized code can
	 * reduce the number of calculations.
	 *
	 * @param singleBand Interpolation for a single band.
	 * @param <T> Single band image trype
	 * @return Interpolation for MultiSpectral images
	 */
	public static <T extends ImageSingleBand> InterpolatePixelMB<MultiSpectral<T>>
	createPixelMB( InterpolatePixelS<T> singleBand ) {
		return new InterpolatePixel_S_to_MB_MultiSpectral<T>(singleBand);
	}

	public static <T extends ImageSingleBand> InterpolatePixelS<T> bilinearPixelS(T image, BorderType borderType) {

		InterpolatePixelS<T> ret = bilinearPixelS((Class) image.getClass(),borderType);
		ret.setImage(image);

		return ret;
	}

	public static <T extends ImageSingleBand> InterpolatePixelS<T> bilinearPixelS(Class<T> imageType, BorderType borderType ) {
		InterpolatePixelS<T> alg;

		if( imageType == ImageFloat32.class )
			alg = (InterpolatePixelS<T>)new ImplBilinearPixel_F32();
		else if( imageType == ImageFloat64.class )
			alg = (InterpolatePixelS<T>)new ImplBilinearPixel_F64();
		else if( imageType == ImageUInt8.class )
			alg = (InterpolatePixelS<T>)new ImplBilinearPixel_U8();
		else if( imageType == ImageSInt16.class )
			alg = (InterpolatePixelS<T>)new ImplBilinearPixel_S16();
		else if( imageType == ImageSInt32.class )
			alg = (InterpolatePixelS<T>)new ImplBilinearPixel_S32();
		else
			throw new RuntimeException("Unknown image type: "+ typeName(imageType));

		if( borderType != null )
			alg.setBorder(FactoryImageBorder.general(imageType, borderType));

		return alg;
	}

	private static String typeName(Class type) {
		return type == null ? "null" : type.getName();
	}

	public static <T extends ImageSingleBand> InterpolateRectangle<T> bilinearRectangle( T image ) {

		InterpolateRectangle<T> ret = bilinearRectangle((Class)image.getClass());
		ret.setImage(image);

		return ret;
	}

	public static <T extends ImageSingleBand> InterpolateRectangle<T> bilinearRectangle( Class<T> type ) {
		if( type == ImageFloat32.class )
			return (InterpolateRectangle<T>)new BilinearRectangle_F32();
		else if( type == ImageUInt8.class )
			return (InterpolateRectangle<T>)new BilinearRectangle_U8();
		else if( type == ImageSInt16.class )
			return (InterpolateRectangle<T>)new BilinearRectangle_S16();
		else
			throw new RuntimeException("Unknown image type: "+typeName(type));
	}

	public static <T extends ImageSingleBand> InterpolatePixelS<T> nearestNeighborPixelS(Class<T> type) {
		if( type == ImageFloat32.class )
			return (InterpolatePixelS<T>)new NearestNeighborPixel_F32();
		else if( type == ImageUInt8.class )
			return (InterpolatePixelS<T>)new NearestNeighborPixel_U8();
		else if( type == ImageSInt16.class )
			return (InterpolatePixelS<T>)new NearestNeighborPixel_S16();
		else if( type == ImageUInt16.class )
			return (InterpolatePixelS<T>)new NearestNeighborPixel_U16();
		else if( type == ImageSInt32.class )
			return (InterpolatePixelS<T>)new NearestNeighborPixel_S32();
		else
			throw new RuntimeException("Unknown image type: "+typeName(type));
	}

	public static <T extends ImageSingleBand> InterpolateRectangle<T> nearestNeighborRectangle( Class<?> type ) {
		if( type == ImageFloat32.class )
			return (InterpolateRectangle<T>)new NearestNeighborRectangle_F32();
//		else if( type == ImageUInt8.class )
//			return (InterpolateRectangle<T>)new NearestNeighborRectangle_U8();
//		else if( type == ImageSInt16.class )
//			return (InterpolateRectangle<T>)new NearestNeighborRectangle_S16();
		else
			throw new RuntimeException("Unknown image type: "+typeName(type));
	}

	public static <T extends ImageSingleBand> InterpolatePixelS<T> bicubicS(float param, float min, float max, Class<T> type) {
		BicubicKernel_F32 kernel = new BicubicKernel_F32(param);
		if( type == ImageFloat32.class )
			return (InterpolatePixelS<T>)new ImplInterpolatePixelConvolution_F32(kernel,min,max);
		else if( type == ImageUInt8.class )
			return (InterpolatePixelS<T>)new ImplInterpolatePixelConvolution_U8(kernel,min,max);
		else if( type == ImageSInt16.class )
			return (InterpolatePixelS<T>)new ImplInterpolatePixelConvolution_S16(kernel,min,max);
		else
			throw new RuntimeException("Unknown image type: "+typeName(type));
	}

	public static <T extends ImageSingleBand> InterpolatePixelS<T> polynomialS(int maxDegree, double min, double max, Class<T> type) {
		if( type == ImageFloat32.class )
			return (InterpolatePixelS<T>)new ImplPolynomialPixel_F32(maxDegree,(float)min,(float)max);
		else if( ImageInteger.class.isAssignableFrom(type) ) {
			return (InterpolatePixelS<T>)new ImplPolynomialPixel_I(maxDegree,(float)min,(float)max);
		} else
			throw new RuntimeException("Unknown image type: "+typeName(type));
	}
}
