SpearWeightedPartials {
	var <partials;
	
	*fromFiles {
		| path |
		var file, contents, contentsArray, partialCount, partialArray,  frameCount, finished,
			partials, freqs, freqsFlop;
		
		file = File.open( path, "r");
		contents = file.readAllString();
		file.close();
		
		contentsArray = contents.split("\n"[0]);
		partialCount =  contentsArray[2].split($ )[1].asInteger;
		frameCount = contentsArray[3].split($ )[1].asInteger;
		
		finished = Condition(false);

		partials = frameCount.collect({
			| i |
			var partialArray, time, splitLine, partialNum, freq, amp; 
			var line = i  + 5;
			partialArray = SparseArray.new;
			splitLine = contentsArray[line].split($ );
			time = splitLine[0].asFloat;

			splitLine[2..].clump(3).do({
				| partial, i |
				partialNum = partial[0].asInteger;
				freq = partial[1].asFloat;
				amp = partial[2].asFloat;
				partialArray[partialNum] = [freq, amp];
			});
			i.postln;
			partialArray;			
		});
		
		^super.newCopyArgs(partials);
	}
	
	getFreqsForTimeRange {
		| start, end |
		var freqs = SparseArray();
		var sortedList = SortedList(50, {| a,b | a[1]>b[1] });
		start = (start*100).asInteger;
		end = (end*100).max(start).asInteger;
	
		(start..end).do({
			| i |
			partials[i].keysValuesDo({
				| index, partial |
				freqs[index].isNil.if({
					freqs[index] = List.new;
				});
				freqs[index].add( partial );
			})
		});
		
		( freqs ? () ).keysValuesDo({
			| index, partials |
			var weight=0, avgFreq=0, count=0;
			partials.do({
				| partial |
				weight = weight + partial[1];
				avgFreq = avgFreq + (partial[1] * partial[0]);
				count = count + 1;
			});
			avgFreq = avgFreq/weight;
			sortedList.add([avgFreq, weight/count ])
		});

		^sortedList.flop;
	} 
}