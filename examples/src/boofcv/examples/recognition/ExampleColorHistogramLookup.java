/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

package boofcv.examples.recognition;

import boofcv.alg.color.ColorHsv;
import boofcv.alg.descriptor.UtilFeature;
import boofcv.alg.feature.color.GHistogramFeatureOps;
import boofcv.alg.feature.color.HistogramFeatureOps;
import boofcv.alg.feature.color.Histogram_F64;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.feature.TupleDesc_F64;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import org.ddogleg.nn.FactoryNearestNeighbor;
import org.ddogleg.nn.NearestNeighbor;
import org.ddogleg.nn.NnData;
import org.ddogleg.struct.FastQueue;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;

import static boofcv.examples.imageprocessing.ExamplePlanarImages.gui;
import static jdk.nashorn.internal.objects.NativeMath.round;

/**
 * Demonstration of how to find similar images using color histograms.  Image color histograms here are treated as
 * features and extracted using a more flexible algorithm than when they are used for image processing.  It's
 * more flexible in that the bin size can be varied and n-dimensions are supported.
 *
 * In this example, histograms for about 150 images are generated.  A target image is selected and the 10 most
 * similar images, according to Euclidean distance of the histograms, are found. This illustrates several concepts;
 * 1) How to construct a histogram in 1D, 2D, 3D, ..etc,  2) Histograms are just feature descriptors.
 * 3) Advantages of different color spaces.
 *
 * Euclidean distance is used here since that's what the nearest-neighbor search uses.  It's possible to compare
 * two histograms using any of the distance metrics in DescriptorDistance too.
 *
 * @author Peter Abeles
 */
public class ExampleColorHistogramLookup {
	/*Static Variablen um in der Inneren Klasse des
	* Onclick Listeners auf die Variablen zuzugreifen bzw diese zu setzen.
	*
	* */
	static int targetOberteil= 0;
	static List<File> imagesOberteile;
	static List<File> imagesHosen;
	static List<File> TestSchuhe;
	static List<double[]> pointsOberteile;
	static List<double[]> pointsHosen;
	static List<double[]> pointsTestSchuhe;
	static double[] targetPoint;
	static File resultHose;
	static File resultSchuhe;
	static ArrayList<File> resultListHose= new ArrayList<File>();
	static ArrayList<File> resultListSchuhe= new ArrayList<File>();

	//Größe der Kleidungstücke, wie sie in der UI angezeigt werden
	private static final int HOSE_WIDTH = 160;
	private static final int HOSE_HEIGHT = 400;
	private static final int OBERTEIL_WIDTH = 300;
	private static final int OBERTEIL_HEIGHT = 400;
	private static final int SCHUHE_WIDTH = 300;
	private static final int SCHUHE_HEIGHT = 150;

	/**
	 * HSV stores color information in Hue and Saturation while intensity is in Value.  This computes a 2D histogram
	 * from hue and saturation only, which makes it lighting independent.
	 */
	public static List<double[]> coupledHueSat( List<File> images  ) {
		List<double[]> points = new ArrayList<>();
		Planar<GrayF32> rgb = new Planar<>(GrayF32.class,1,1,3);
		Planar<GrayF32> hsv = new Planar<>(GrayF32.class,1,1,3);

		for( File f : images ) {
			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null ) throw new RuntimeException("Can't load image!");

			rgb.reshape(buffered.getWidth(), buffered.getHeight());
			hsv.reshape(buffered.getWidth(), buffered.getHeight());

			ConvertBufferedImage.convertFrom(buffered, rgb, true);
			ColorHsv.rgbToHsv_F32(rgb, hsv);

			Planar<GrayF32> hs = hsv.partialSpectrum(0,1);

			// The number of bins is an important parameter.  Try adjusting it
			Histogram_F64 histogram = new Histogram_F64(12,12);
			histogram.setRange(0, 0, 2.0*Math.PI); // range of hue is from 0 to 2PI
			histogram.setRange(1, 0, 1.0);         // range of saturation is from 0 to 1

			// Compute the histogram
			GHistogramFeatureOps.histogram(hs,histogram);

			UtilFeature.normalizeL2(histogram); // normalize so that image size doesn't matter

			points.add(histogram.value);
		}

		return points;
	}

	/**
	 * Computes two independent 1D histograms from hue and saturation.  Less affects by sparsity, but can produce
	 * worse results since the basic assumption that hue and saturation are decoupled is most of the time false.
	 */
	public static List<double[]> independentHueSat( List<File> images  ) {
		List<double[]> points = new ArrayList<>();

		// The number of bins is an important parameter.  Try adjusting it
		TupleDesc_F64 histogramHue = new TupleDesc_F64(30);
		TupleDesc_F64 histogramValue = new TupleDesc_F64(30);

		List<TupleDesc_F64> histogramList = new ArrayList<>();
		histogramList.add(histogramHue); histogramList.add(histogramValue);

		Planar<GrayF32> rgb = new Planar<>(GrayF32.class,1,1,3);
		Planar<GrayF32> hsv = new Planar<>(GrayF32.class,1,1,3);

		for( File f : images ) {
			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null ) throw new RuntimeException("Can't load image!");

			rgb.reshape(buffered.getWidth(), buffered.getHeight());
			hsv.reshape(buffered.getWidth(), buffered.getHeight());
			ConvertBufferedImage.convertFrom(buffered, rgb, true);
			ColorHsv.rgbToHsv_F32(rgb, hsv);

			GHistogramFeatureOps.histogram(hsv.getBand(0), 0, 2*Math.PI,histogramHue);
			GHistogramFeatureOps.histogram(hsv.getBand(1), 0, 1, histogramValue);

			// need to combine them into a single descriptor for processing later on
			TupleDesc_F64 imageHist = UtilFeature.combine(histogramList,null);

			UtilFeature.normalizeL2(imageHist); // normalize so that image size doesn't matter

			points.add(imageHist.value);
		}

		return points;
	}

	/**
	 * Constructs a 3D histogram using RGB.  RGB is a popular color space, but the resulting histogram will
	 * depend on lighting conditions and might not produce the accurate results.
	 */
	public static List<double[]> coupledRGB( List<File> images ) {
		List<double[]> points = new ArrayList<>();

		Planar<GrayF32> rgb = new Planar<>(GrayF32.class,1,1,3);

		for( File f : images ) {
			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null ) throw new RuntimeException("Can't load image!");

			rgb.reshape(buffered.getWidth(), buffered.getHeight());
			ConvertBufferedImage.convertFrom(buffered, rgb, true);

			// The number of bins is an important parameter.  Try adjusting it
			Histogram_F64 histogram = new Histogram_F64(80,80,80);
			histogram.setRange(0, 0, 255);
			histogram.setRange(1, 0, 255);
			histogram.setRange(2, 0, 255);

			GHistogramFeatureOps.histogram(rgb,histogram);

			UtilFeature.normalizeL2(histogram); // normalize so that image size doesn't matter

			points.add(histogram.value);
		}

		return points;
	}

	/**
	 * Computes a histogram from the gray scale intensity image alone.  Probably the least effective at looking up
	 * similar images.
	 */
	public static List<double[]> histogramGray( List<File> images ) {
		List<double[]> points = new ArrayList<>();

		GrayU8 gray = new GrayU8(1,1);
		for( File f : images ) {
			BufferedImage buffered = UtilImageIO.loadImage(f.getPath());
			if( buffered == null ) throw new RuntimeException("Can't load image!");

			gray.reshape(buffered.getWidth(), buffered.getHeight());
			ConvertBufferedImage.convertFrom(buffered, gray, true);

			TupleDesc_F64 imageHist = new TupleDesc_F64(150);
			HistogramFeatureOps.histogram(gray, 255, imageHist);

			UtilFeature.normalizeL2(imageHist); // normalize so that image size doesn't matter

			points.add(imageHist.value);
		}

		return points;
	}
	/**
	* Filelisten initialisieren und in punktlisten umwandeln dort können verschiedene arten der farbhistogramme
	* gewählt werden. Danach werden dem UI alle Oberteile hinzugefügt und die verschiedenen Buttons initialisiert
	 *
	* */

	public static void main(String[] args) {
		String regexImage = ".+jpg";
		String pathOberteile = UtilIO.pathExample("recognition/Oberteile");
		String pathHosen = UtilIO.pathExample("recognition/Hosen");
		String pathSchuhe = UtilIO.pathExample("recognition/Schuhe");
		//List <File> images für Oberteile
		//List <File> images für hosen/Unterteile
		//List <File> image für Schuhe
		imagesHosen = Arrays.asList(BoofMiscOps.findMatches(new File(pathHosen), regexImage));
		Collections.sort(imagesHosen);
		imagesOberteile = Arrays.asList(BoofMiscOps.findMatches(new File(pathOberteile), regexImage));
		Collections.sort(imagesOberteile);
		TestSchuhe = Arrays.asList(BoofMiscOps.findMatches(new File(pathSchuhe), regexImage));
		Collections.sort(TestSchuhe);
		// Different color spaces you can try
		// Color listen für hose und schuhe anlegen lassen
		pointsOberteile = coupledRGB(imagesOberteile);
		pointsHosen = coupledRGB(imagesHosen);
		pointsTestSchuhe = coupledRGB(TestSchuhe);
//		List<double[]> points = independentHueSat(images);
//		List<double[]> points = coupledRGB(images);
//		List<double[]> points = histogramGray(images);
		final ListDisplayPanel gui = new ListDisplayPanel();
		// Fügt dem UI alle Oberteile hinzu
		for (int i = 0; i < imagesOberteile.size(); i++) {
			File file = imagesOberteile.get(i);
			BufferedImage image = UtilImageIO.loadImage(file.getPath());
			gui.addImage(resize(image, OBERTEIL_WIDTH, OBERTEIL_HEIGHT), String.format("Oberteil Nummer " + (i+1)), ScaleOptions.NONE);}
		//Zeigt UI an
		ShowImages.showWindow(gui,"Similar Images",true);
		// Holt Button aus der Klasse
		setButtonOberteile(gui);
		setButtonHosen(gui);
		setButtonSchuhe(gui);
	}
	/**
	*
	* Initialisierung der verschiedenen Buttons, die beim klicken Ähnlichkeit aufgrund des ausgewählten kleidungsstück berechnen
	* und die 3 nächsten nachbarn im UI zurückgeben
	* Der letzte Button gibt das gesamte Outfit aus
	*
	* */

	private static void setButtonOberteile(final ListDisplayPanel gui) {
		final JButton buttonHose = gui.getJButtonHose();
		buttonHose.setVisible(false);
		final JButton button = gui.getJButton();
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Oberteil Nummer "+gui.getImage()+" wurde ausgewählt!");
				targetOberteil = gui.getImage() ;
				gui.reset();
				NearestNeighbor<File> nn = FactoryNearestNeighbor.exhaustive();
				FastQueue<NnData<File>> results = new FastQueue(NnData.class,true);
				targetPoint = pointsOberteile.get(targetOberteil);
				nn.init(targetPoint.length);
				nn.setPoints(pointsHosen, imagesHosen);
				nn.findNearest(targetPoint, -1, 20, results);
				for (int i = 0; i < results.size; i++) {
					File file = results.get(i).data;
					double error = results.get(i).distance;
					BufferedImage image = UtilImageIO.loadImage(file.getPath());
					gui.addImage(resize(image, HOSE_WIDTH, HOSE_HEIGHT), "Hose Nummer " + (i+1) + "(" + error +")", ScaleOptions.NONE);
					resultListHose.add(file);
					System.out.println("Die ResultListHose ist "+resultListHose.size()+" Elemente groß");

				}
				button.setVisible(false);
				buttonHose.setVisible(true);

			}
		});

	}
	private static void setButtonHosen(final ListDisplayPanel gui) {
		final JButton buttonSchuhe = gui.getJButtonSchuhe();
		final JButton button = gui.getJButtonHose();
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				resultHose = resultListHose.get(gui.getImage());
				System.out.println("Hose Nummer "+gui.getImage()+" wurde ausgewählt!");
				gui.reset();
				NearestNeighbor<File> nn = FactoryNearestNeighbor.exhaustive();
				FastQueue<NnData<File>> results = new FastQueue(NnData.class,true);
				targetPoint = pointsOberteile.get(targetOberteil);
				nn.init(targetPoint.length);
				nn.setPoints(pointsTestSchuhe, TestSchuhe);
				nn.findNearest(targetPoint, -1, 10, results);
				for (int i = 0; i < results.size; i++) {
					File file = results.get(i).data;
					double error = results.get(i).distance;
					BufferedImage image = UtilImageIO.loadImage(file.getPath());
					gui.addImage(resize(image, SCHUHE_WIDTH, SCHUHE_HEIGHT), "Schuh Nummer " + (i+1) + "(" + error +")", ScaleOptions.NONE);
					resultListSchuhe.add(file);
					System.out.println("ResultList Schuhe ist "+resultListSchuhe.size()+" Elemente groß");
				}
				button.setVisible(false);
				buttonSchuhe.setVisible(true);
			}
		});

	}
	private static void setButtonSchuhe(final ListDisplayPanel gui){
		final JButton button = gui.getJButtonSchuhe();
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				resultSchuhe = resultListSchuhe.get(gui.getImage());
				System.out.println("Schuh Nummer "+gui.getImage()+" wurde ausgewählt");
				gui.reset();
				File fileOberteil = imagesOberteile.get(targetOberteil);
				BufferedImage imageOberteil = UtilImageIO.loadImage(fileOberteil.getPath());
				BufferedImage imageHose = UtilImageIO.loadImage(resultHose.getPath());
				BufferedImage imageSchuhe = UtilImageIO.loadImage(resultSchuhe.getPath());
				gui.addImage(resize(imageOberteil, OBERTEIL_WIDTH, OBERTEIL_HEIGHT), String.format("Oberteil"), ScaleOptions.NONE);
				gui.addImage(resize(imageHose, HOSE_WIDTH, HOSE_HEIGHT), String.format("Hose"), ScaleOptions.NONE);
				gui.addImage(resize(imageSchuhe, SCHUHE_WIDTH, SCHUHE_HEIGHT), String.format("Schuhe"), ScaleOptions.NONE);
				button.setVisible(false);
			}
		});

	}

	public static BufferedImage resize(BufferedImage img, int newW, int newH) {
		Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
		BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g2d = dimg.createGraphics();
		g2d.drawImage(tmp, 0, 0, null);
		g2d.dispose();

		return dimg;
	}



}
