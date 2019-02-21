package app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import ac.ArithmeticEncoder;
import io.OutputStreamBitSink;

public class PriorValueContextAdaptiveACEncodeVideoFile {
	
	public static void main(String[] args) throws IOException {	
		String input_file_name = "data/out.dat";
		String output_file_name = "data/prior-value-context-adaptive-compressed-out.dat";
		
		int range_bit_width = 40;

		System.out.println("Encoding video file: " + input_file_name);
		System.out.println("Output compressed dat: " + output_file_name);
		System.out.println("Range Register Bit Width: " + range_bit_width);

		int num_pixels = (int) new File(input_file_name).length();
				
		Integer[] differences = new Integer[511]; //Possible values: from -255 to +255
		for (int i=0; i<differences.length; i++) {
			//int index = i+255;
			differences[i] = i-255;
			//System.out.println("differences[i] " + i + " is " + (i-255));
		}

		// Create 64 * 64 = 4096 models for each pixel position in one frame. 
		// Model chosen depends on value of pixel value in the prior frame.
		
		PriorValuePixelModel[] models = new PriorValuePixelModel[4096];
		
		for (int i=0; i<4096; i++) {
			// Create new model with default count of 1 for all symbols
			// TODO: another way???
			models[i] = new PriorValuePixelModel(differences);
		}

		ArithmeticEncoder<Integer> encoder = new ArithmeticEncoder<Integer>(range_bit_width);

		FileOutputStream fos = new FileOutputStream(output_file_name);
		OutputStreamBitSink bit_sink = new OutputStreamBitSink(fos);

		// First 4 bytes are the number of pixels encoded
		bit_sink.write(num_pixels, 32);		

		// Next byte is the width of the range registers
		bit_sink.write(range_bit_width, 8);

		// Now encode the input
		FileInputStream fis = new FileInputStream(input_file_name);
		
		// Use model 0 as initial model + initiate the frame
		PriorValuePixelModel model = models[0];
		int[] lastFrame = new int[4096];
		
		// Get the first 4096 pixels as the base current_frame
		for (int i=0; i<4096; i++) {
			int next_pixel = fis.read();
			
			lastFrame[i] = next_pixel;
			model = models[i];
			
			encoder.encode(next_pixel, model, bit_sink);
			
			System.out.println("Save the 1st frame " + i + ": " + next_pixel);
		}

		for (int i=4096; i<num_pixels; i++) {
			int next_pixel = fis.read();
			int absoluteIndex = i % 4096;
			model = models[absoluteIndex];
			
			System.out.println("The current pixel is " + next_pixel);
			
			// Encoding and updating the difference
			Integer difference = next_pixel - lastFrame[absoluteIndex];
			//System.out.println("The current pixel difference is " + difference);
			//difference += 255; //For handling negative numbers
			encoder.encode(difference, model, bit_sink);
			
			// Update model used
			model.updateCount(difference);
			
			// Update current frame
			lastFrame[absoluteIndex] = next_pixel;
			
			// Set up next model based on symbol just encoded
			// model = models[next_pixel];
			
		}
		
		
		fis.close();

		// Finish off by emitting the middle pattern 
		// and padding to the next word
		
		encoder.emitMiddle(bit_sink);
		bit_sink.padToWord();
		fos.close();
		
		System.out.println("Done");
	}
}
