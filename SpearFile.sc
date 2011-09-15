//SpearFile {
//	classvar <>maxLineLength = 10000;
//	var server, <file, <partials, <partialCount, <frameCount;
//	var <buffer, <bufnum;
//	var <>analysisWindowSize=1024;
//	
//	*new {
//		| path, server |
//		^super.new.init( path, server )
//	}
//	
//	init {
//		|inPath, inServer|
//		var partialsCount, partialTracker, lastFrame, thisFrame, amps, freqs, phases;
//		var atsBuffer, partialsByFrame, inc, f, n;
//		
//		server = inServer ? Server.default;
//		
//		lastFrame = List.new; thisFrame = List.new;
//		
//		partialTracker = List.new;
//		partials = List.new;
//
//		f = File( inPath,"r") ;
//		
//		// First three lines are unused. This is not a good way to parse the files but, whatever;
//		n = f.getLine; n = f.getLine; n = f.getLine;
//		
//		partialsCount = n[n.findRegexp(" ")[0][0]..n.size].asInteger;
//
//			n = f.getLine;
//		frameCount = n[n.findRegexp(" ")[0][0]..n.size].asInteger;
//			f.getLine;
//		
//		frameCount.do({
//			| frame |
//			var data, time, numPartials, partialList;
//			data = f.getLine(maxLineLength).split($ );
//			time = data.removeAt(0);
//			numPartials = data.removeAt(0);
//			partialList = data.clump(3);
//					
//			partialList.do({
//				| pdata |
//				var parNum, freq, amp, free, slot;
//				if( pdata[2].isNil, {pdata.postln} );
//				parNum = pdata[0].asInteger;
//				freq = pdata[1].asFloat;
//				amp = pdata[2].asFloat;
//				thisFrame.add( parNum );
//				if( lastFrame.indexOf( parNum ).notNil, {
//					// was around in the prev. frame
//					slot = partialTracker.indexOf( parNum );
//				},{
//					// Not around in previous frame
//					free = partialTracker.indexOf( nil );		
//					free.isNil.if({
//						partialTracker.add( parNum );
//						partials.add( Array.newClear( frameCount ).fill([400,0]) );						slot = partialTracker.size-1;
//						//format("Increasing size of array to %.", partialTracker.size ).postln;
//					},{
//						slot = free;
//						partialTracker[free] = parNum;
//					});
//				});
//				// Add data to slot;
//				partials[slot][frame] = [freq,amp];
//			});
//			lastFrame.difference(thisFrame).do({
//				| deadPartial |
//				//format("removing partial:%",deadPartial ).postln;
//				partialTracker[ partialTracker.indexOf( deadPartial ) ] = nil;
//			});
//			lastFrame = thisFrame;
//			thisFrame = List.new;
//		});
//		partialCount = partials.size;
//	}
//	
//	loadToBuffer {
//		| action |
//		var atsBuffer, amps, freqs, phases, inc;
//		// Pretty sure I got the size right here....
//		atsBuffer = DoubleArray.newClear( 10 + (partialCount * frameCount * 3) + (frameCount*25) );
//		
//		// generate amp list from partials: [ partial1:[t0-amp, t1-amp, t2-amp....], partial2:[...], partial3:[].... ]
//		amps = partials.collect({ 
//			| par |
//			par.collect({
//				|frame|
//				frame[1]
//			})
//		});
//		// generate freq list
//		freqs = partials.collect({ 
//			| par |
//			par.collect({
//				|frame|
//				frame[0]
//			})
//		});
//		// We get no phase data, so fill phase list with 0's
//		phases = Array.newClear(partials.size).fill( Array.newClear(frameCount).fill(0) );
//		
//		// Write ats header info to the buffer;
//		/*
//		atsBuffer[0] = 4;
//		atsBuffer[1] = partials.size;
//		atsBuffer[2] = frameCount;
//		atsBuffer[3] = analysisWindowSize;		// Window size of analysis? Not sure how this affects things, but set it to 1024 by def.
//		*/
//		atsBuffer[0] = 123;
//		atsBuffer[1] = 44100;
//		atsBuffer[2] = 1024;
//		atsBuffer[3] = 1024;
//		atsBuffer[4] = partialCount;
//		atsBuffer[5] = frameCount;
//		atsBuffer[6] = 1;
//		atsBuffer[7] = partials.flop[0].flop[0].maxItem;
//		atsBuffer[8] = frameCount*0.01;
//		atsBuffer[9] = 4;
//		
//		// Copied from AtsFile
//		inc  = numPartials * 3 + 26;
//		frameCount.do({
//			| frame |
//			var freq, amp, phase, time;
//			time = frame * 0.10;
//			atsBuffer[10 + (frame*inc)] = time;
//			
//			partials.size.do({
//				| partial |
////				atsBuffer[10 + (frame*inc + 2 + (partial*offset))] = partials[partial][frame]
//			})			
//		})
//
//			amps.do({
//				| pars |
//				pars.do({
//					| amp |
//					atsBuffer[inc] = amp;
//					inc = inc+1;
//				})
//			});
//			freqs.do({
//				| pars |
//				pars.do({
//					| freq |
//					atsBuffer[inc] = freq;
//					inc = inc+1;
//				})
//			});
//			phases.do({
//				| pars |
//				pars.do({
//					| phase |
//					atsBuffer[inc] = phase;
//					inc = inc+1;
//				})
//			});			
//		// noise bands
//		// Fill these with 0's for now - we can possibly generate a residual later and use that to fill these
//		Array.newClear(frameCount).fill( Array.newClear(25).fill(0) ).do({
//			| pars |
//			pars.do({
//				| noi |
//				atsBuffer[inc] = noi;
//				inc = inc+1;
//			})	
//		});
//		
//		atsBuffer.postln;
//		
//		Buffer.loadCollection( server, atsBuffer, action:{ |b| b.postln; action.value(b); bufnum = b.bufnum; buffer=b} );
//		^atsBuffer;
//	}
//}
