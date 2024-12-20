/*
* Copyright 2024 Sly Technologies Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.slytechs.jnet.platform.examples.pipeline;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.slytechs.jnet.platform.api.pipeline.DataLiteral;
import com.slytechs.jnet.platform.api.pipeline.Pipeline;
import com.slytechs.jnet.platform.examples.pipeline.SimplePipeline.SimpleProcessor;

/**
 * A demonstration pipeline that processes character data through a series of
 * text transformations. The pipeline demonstrates common patterns for building
 * data processing chains using the Pipeline framework.
 *
 * <p>
 * Each input character array flows through the following transformation stages:
 * <ol>
 * <li>Conversion from char[] to StringBuilder</li>
 * <li>Prepending "My " to the text</li>
 * <li>Converting text to uppercase</li>
 * <li>Appending " Friends" to the text</li>
 * <li>Final conversion to String output</li>
 * </ol>
 *
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * var pipeline = new SimplePipeline();
 * 
 * // Connect output handler
 * pipeline.out("SimpleOutput", (SimpleOutput) result -> System.out.println("Result: " + result));
 * 
 * // Process data through pipeline
 * pipeline.in("SimpleInput").accept("hello".toCharArray());
 * // Outputs: "MY HELLO FRIENDS"
 * }</pre>
 *
 * @author Mark Bednarczyk
 * @see Pipeline
 * @see DataLiteral
 */
public class SimplePipeline extends Pipeline<SimpleProcessor> {

	/**
	 * Input stage of the pipeline that accepts character array data. Converts the
	 * input char[] to a StringBuilder for processing.
	 */
	interface SimpleInput extends Consumer<char[]> {
	}

	/**
	 * Intermediate processing stage that transforms StringBuilder content. Each
	 * processor in the pipeline chain implements this interface to perform text
	 * transformations.
	 */
	interface SimpleProcessor extends Consumer<StringBuilder> {
	}

	/**
	 * Output stage that receives the final processed text as a String. Connected
	 * outputs will receive the fully transformed text after all pipeline stages
	 * complete.
	 */
	interface SimpleOutput extends Consumer<String> {
	}

	/**
	 * Creates the input stage processor that converts char[] input to
	 * StringBuilder.
	 *
	 * @param next supplier for the next processor in the chain
	 * @return a SimpleInput processor that forwards to the next stage
	 */
	private SimpleInput input(Supplier<SimpleProcessor> next) {
		return (char[] chars) -> next.get().accept(new StringBuilder(new String(chars)));
	}

	/**
	 * Creates the output stage processor that converts StringBuilder to String.
	 *
	 * @param out supplier for the output consumer
	 * @return a SimpleProcessor that forwards to the output stage
	 */
	private SimpleProcessor output(Supplier<SimpleOutput> out) {
		return (StringBuilder sb) -> out.get().accept(sb.toString());
	}

	/**
	 * Creates a processor that converts text to uppercase.
	 *
	 * @param next supplier for the next processor in the chain
	 * @return a SimpleProcessor that performs uppercase conversion
	 */
	private SimpleProcessor toUppercase(Supplier<SimpleProcessor> next) {
		return (StringBuilder sb) -> {
			for (int i = 0; i < sb.length(); i++) {
				char c = sb.charAt(i);
				char upperChar = Character.toUpperCase(c);

				sb.setCharAt(i, upperChar);
			}

			next.get().accept(sb);
		};
	}

	/**
	 * Constructs a new SimplePipeline with predefined processing stages.
	 * Initializes the pipeline with:
	 * <ul>
	 * <li>Input stage for char[] to StringBuilder conversion</li>
	 * <li>Processing stage to add "My " prefix</li>
	 * <li>Processing stage for uppercase conversion</li>
	 * <li>Processing stage to append " Friends"</li>
	 * <li>Output stage for StringBuilder to String conversion</li>
	 * </ul>
	 */
	public SimplePipeline() {
		super("simple-pipeline", new DataLiteral<>(SimpleProcessor.class));

		/* Setup an input and output using method references */
		head().addInput("SimpleInput", this::input, new DataLiteral<>() {});
		tail().addOutput(0, "SimpleOutput", this::output, new DataLiteral<>() {});

		/* Add processor method reference, with priority 1 */
		this.addProcessor(1, "ToUpper", this::toUppercase);
		
		/* Add a lambda processors at different priorities */
		this.addProcessor(0, "Insert(My)", next -> sb -> next.get().accept(sb.insert(0, "My ")));
		this.addProcessor(2, "Append(Friends)", next -> sb -> next.get().accept(sb.append(" Friends")));
	}

	/**
	 * Main demonstration method showing different ways to use the pipeline.
	 * Demonstrates:
	 * <ul>
	 * <li>Pipeline construction</li>
	 * <li>Output connection using lambda</li>
	 * <li>Input processing using direct Consumer access</li>
	 * <li>Input processing using lambda forwarding</li>
	 * </ul>
	 * 
	 * @param args command line arguments (not used)
	 */
	public static void main(String[] args) {
		var pipeline = new SimplePipeline();

		System.out.println(pipeline.toStringInOut());

		// Connect SimpleOutput as lambda
		pipeline.out("SimpleOutput", (SimpleOutput) result -> System.out.printf("out=%s%n", result));

		// Get as a return value of type SimpleInput/Consumer<char[]>
		pipeline.in("SimpleInput", SimpleInput.class).accept("Fellow".toCharArray());

		// Get as an lambda action/consumer within, of SimpleInput which forward on
		pipeline.in("SimpleInput", (SimpleInput in) -> in.accept("Best".toCharArray()));

		/* Generated output: */
		var _ = """
simple-pipeline: {SimpleInput} → Insert(My):0 → ToUpper:1 → Append(Friends):2 → {SimpleOutput}
out=MY FELLOW Friends
out=MY BEST Friends
				""";
	}
}
