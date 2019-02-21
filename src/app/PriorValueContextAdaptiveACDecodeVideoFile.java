package app;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import ac.ArithmeticDecoder;
import io.InputStreamBitSource;
import io.InsufficientBitsLeftException;

public class PriorValueContextAdaptiveACDecodeVideoFile {
	public static void main(String[] args) throws InsufficientBitsLeftException, IOException {
		String input_file_name = "data/prior-value-context-adaptive-compressed-out.dat";
		String output_file_name = "data/prior-value-context-adaptive-reuncompressed-out.dat";

		FileInputStream fis = new FileInputStream(input_file_name);

		InputStreamBitSource bit_source = new InputStreamBitSource(fis);

		Integer[] differences = new Integer[511]; //Possible values: from -255 to +255
		for (int i=0; i<differences.length; i++) {
			differences[i] = i-255;
			//System.out.println("differences[i] " + i + " is " + (i-255));
		}
		
		PriorValuePixelModel[] models = new PriorValuePixelModel[4096];
		
		for (int i=0; i<4096; i++) {
			// Create new model with default count of 1 for all symbols
			models[i] = new PriorValuePixelModel(differences);
		}
		
		// Read in number of symbols encoded
		
		int num_pixels = bit_source.next(32);

		// Read in range bit width and setup the decoder
		
		int range_bit_width = bit_source.next(8);
		ArithmeticDecoder<Integer> decoder = new ArithmeticDecoder<Integer>(range_bit_width);

		// Decode and produce output.
		
		System.out.println("Uncompressing dat file: " + input_file_name);
		System.out.println("Output dat file: " + output_file_name);
		System.out.println("Range Register Bit Width: " + range_bit_width);
		System.out.println("Number of decoded pixels: " + num_pixels);
		
		FileOutputStream fos = new FileOutputStream(output_file_name);

		// Use model 0 as initial model.
		PriorValuePixelModel model = models[0];
		int[] lastFrame = new int[4096];
		
		// Get the first 4096 pixels as the base current_frame
		for (int i=0; i<4096; i++) {
			model = models[i];
			
			int next_pixel = decoder.decode(model, bit_source); 
			
			System.out.println("Decoded 1st frame pixel " + i +" with value" + next_pixel);
			
			fos.write(next_pixel);
			
			lastFrame[i] = next_pixel;
		}

		for (int i=4096; i<num_pixels; i++) {
			// Get the position and the model
			int absoluteIndex = i % 4096;
			model = models[absoluteIndex];
			
			int next_diff = decoder.decode(model, bit_source);
						
			// Decoding: get the actual pixel value
			Integer pixel = next_diff + lastFrame[absoluteIndex];
			
			fos.write(pixel);
			System.out.println("Decoded following pixel: " + i +" with value " + pixel);
			
			// Update model used
			model.updateCount(next_diff);
			
			// Update current frame
			lastFrame[absoluteIndex] = pixel;
		}

		System.out.println("Done.");
		fos.flush();
		fos.close();
		fis.close();
	}
}
