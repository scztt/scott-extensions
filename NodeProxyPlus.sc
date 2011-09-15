+ NodeProxy {
/*
	SynthDef( \line, { 
		arg  out=0, min=0.0, max=1.0, target=0.7, fadeTime=10;
		var time, gate=0, cur, distance, last, sig, phase;
		target = min + (target*(max-min));
		cur = LocalIn.kr( 1 );
		gate = Slope.kr( target ).abs;
		phase = Phasor.kr( gate, Trig1.kr( gate, fadeTime-(64/44100) )/(fadeTime*44100/64), 0, 1.01, 0 );
		cur = Latch.kr( cur, gate );
		sig = cur + (phase * (target-cur) );
		LocalOut.kr( sig );
		Out.kr( out, sig );
	}).send(s).store;
*/

	makeNodeDisplay {
		| page, symbol, bgcolor, ignoredControls |
		var parentPage, playButton, showControlsButton, controlMappings;
		ignoredControls = ignoredControls ? [ \out, \gate, \fadeTime ];
		
		parentPage = page;
		parentPage.startRow;
		
		page = FlowView( page, page.layRight( 500, 20) );
		page.background_( bgcolor );
		bgcolor.postln;

		// showControls
		showControlsButton = SCButton( page, page.layRight( 20, 18 ) )
			.font_(Font("Helvetica",9) )
			.states_([ ["->", Color.black, Color.grey(0.7) ],
					  ["X", Color.black, Color.grey(0.7) ] ] )
			.action_({
				| button |
				if( button.value == 1, {
					controlMappings = this.makeControlMappingBox( page, ignoredControls );
					page.resizeToFit(true, true);
				},{
					controlMappings.remove; controlMappings = nil;
					page.bounds_( page.bounds.height_(20).width_(500) ).reflowAll;
					parentPage.reflowAll;
				})
			});
		
		// name
		ActionButton.new( page, "~" + symbol.asString + "." + (bus.rate==\control).if({"kr"},{"ar"}) + 
			"(" + bus.numChannels + ")", minHeight: 18, backcolor: Color.grey(0.7) );
		
		// play button
		playButton = (bus.rate==\audio).if({ SCButton( page, page.layRight( 20,18 ) )
			.states_([[">", Color.green, Color.grey(0.7)],["||",Color.blue, Color.grey(0.7)]])
			.action_({ | button |
				( button.value==0 ).if({
					this.stop;
				},{
					this.play;
				})
			}); 
		});
		page.startRow;
		monitor.isNil.not.if({ 
			(monitor.synthIDs.size>0).if({
				playButton.value = 1;
			})
		});
				
		
		//page.resizeToFit;
		parentPage.reflowAll;
		^page
	}

	makeControlMappingBox {
		| page, ignoredControls |
		var box;
		box = FlowView( page, page.layRight( 400, 90 ) );
		this.getAllControls( ignoredControls ).do({
			| cont |
			box.startRow;
			SCDragSink( box, box.layRight( 30, 15 ) )
				.string_( this.nodeMap[cont.name].isNil.if({
					"(" ++ cont.defaultValue ++ ")"
				},{
					"(" ++ this.nodeMap[cont].value ++ ")"
				}) )
				.align_(\right)
				.font_( Font("Helvetica", 9 ) )
				.canReceiveDragHandler_({
					SCDragSink.currentDrag.class == ControlNode;
				})
				.receiveDragHandler_({
					| sink |
					this.map( cont.name, BusPlug.for(SCDragSink.currentDrag.bus) );
					sink.string_( SCDragSink.currentDrag.name);
					sink.background_( SCDragSink.currentDrag.color );
				})
				.mouseDownAction_({
					| sink |
					this.unmap( \freq );
					sink.background_(Color(1,1,1,0))
						.string_( this.nodeMap[cont.name].isNil.if({
							"(" ++ cont.defaultValue ++ ")"
						},{
							"(" ++ this.nodeMap[cont].value ++ ")"
						}) )
				});
			SCStaticText( box, box.layRight( 60, 15 ) )
				.string_( "-> " ++ cont.name.asString )
				.font_( Font( "Helvetica", 9 ) );
		});
		box.resizeToFit(true);
		^box
	}
	
	getAllControls {
		| ignoredControls |
		var allControls;
		ignoredControls = ignoredControls ? [];
		allControls = List(0);
		objects.do({
			| obj |
			obj.controlNames.do({
				| contName |
				ignoredControls.includes( contName.name ).not.if({
					allControls.add( contName );
				})
			})
		});
		^allControls
	}

	*busEnvelope {
		| s, initial=0.0, min=0.0, max=1.0, name |
		var spaceName, now=0, maxValue, minValue, maxT=100, v, t, vRt, tRt, a, b, n, r, warp, bus, clock, synth;
		
		s = s ? Server.default;
		bus = NodeProxy.new(s,\control, 1).source_( \line, 0, [\target, 0.1, \fadeTime, 3, \min, min, \max, max] );
		initial = (initial-min)/(max-min);
		
		v = [ initial, initial ];
		t = [ 0, 9999];
		vRt= v; tRt = t;
		

		a = PageLayout.new( "p[" ++ currentEnvironment.name ++ "][" ++ name ++ "]", 750 );
		a.onClose_({ r.stop; n.stop; clock.clear });
		
		b = SCEnvelopeView(a.window, a.layRight(600, 80))
			.drawLines_(true)
			.fillColor_(Color.white)
			.drawRects_(true)
			.value_([t/maxT , v]);
		
		b.mouseDownAction = { 
			arg a,x,y; 
			var newT, newV, nowIndex, newIndex;
			newT = clock.beats + ( (x / b.bounds.width)**2 * maxT);
			newV = (b.bounds.height - y) / b.bounds.height;
			nowIndex = t.indexInBetween( clock.beats );
			newIndex = t.indexInBetween( newT );
			
			newIndex = newIndex.ceil.asInt;
			if( newT < t[nowIndex.ceil.asInt] , { 
				v = v.insert (newIndex, newV);
				t = t.insert (newIndex, newT);
				v[v.size-1] = v[v.size-2];
				n.stop.reset; clock.clear; n.play(clock,0); r.play(clock,0);
			}, {
				v = v.insert (newIndex, newV);
				t = t.insert (newIndex, newT);
				v[v.size-1] = v[v.size-2];	
			});
		};
		
		maxValue = SCNumberBox( a.window, a.layDown(70, 20 ) )
			.value_(max)
			.action_( { |n| bus.set( \max, n.value )} ); 
		
		minValue = SCNumberBox( a.window, a.layRight(70, 20 ) )
			.value_(min)
			.action_( { |n| bus.set( \min, n.value ) } ); 
			
		a.resizeToFit;
		a.front;
		b.resize_(1);
		
		r = Routine({ arg inval;
			var vNow, nowIndex, tNow, curV=0, curT=0;
			curV = v[0]; curT = t[0];
			loop {
				now = thisThread.beats;
				nowIndex = t.indexInBetween( now );
		
				vNow = [v.blendAt( nowIndex )] ++ v.copyRange( nowIndex.ceil.asInt, 9999);
				tNow = [t.blendAt( nowIndex )] ++ t.copyRange( nowIndex.ceil.asInt, 9999);
					tNow = tNow/maxT;
					tNow = tNow - tNow.first + 0.000000001;
				{
					b.thumbSize_( 2 );
					tNow.size.do({
						arg i;
						b.value_( [ tNow**0.5 , vNow] );
					//	b.setString( i, vNow[i].asString );
					});
				}.defer;
				0.1.yield;		
			}
		});
		
		clock = TempoClock.new;
		
		n = Routine ({ arg inval;	
			var now, time, index;
			now = thisThread.beats;
			index = t.indexInBetween( now ).ceil.asInt;
			time = t[index] - now;
			bus.set( \target, v[index], \fadeTime, time );
			time.yield;
			
			loop {
				time = t[index+1] - t[index];
				bus.set( \target, v[index+1], \fadeTime, time );
				index = index + 1;
				time.yield;
			}
		} );
		n.play(clock); r.play(clock);
		^bus;
	}

	busEnvelope {
		| min=0.0, max=1.0 |
		var now=0, maxValue, minValue, maxT=100, v, t, vRt, tRt, a, b, n, r, warp, bus, clock, synth;

		(this.source == \line).if( {
			v = [ 0.5, 0.5 ];
			t = [ 0, 9999];
			this.bus.get( { 
				|val| 
				val = (val-min)/(max-min);
				v = [ val, val ];
				t = [0, 9999 ];
			});

			bus = this;

			vRt = v; tRt = t;	
			
			a = PageLayout.new( "envelope", 750 );
			a.onClose_({ r.stop; n.stop; clock.clear });
			
			b = SCEnvelopeView(a.window, a.layRight(600, 80))
				.drawLines_(true)
				.fillColor_(Color.white)
				.drawRects_(true)
				.value_([t/maxT , v]);
			
			b.mouseDownAction = { 
				arg a,x,y; 
				var newT, newV, nowIndex, newIndex;
				newT = clock.beats + ( (x / b.bounds.width)**2 * maxT);
				newV = (b.bounds.height - y) / b.bounds.height;
				nowIndex = t.indexInBetween( clock.beats );
				newIndex = t.indexInBetween( newT );
				
				newIndex = newIndex.ceil.asInt;
				if( newT < t[nowIndex.ceil.asInt] , { 
					v = v.insert (newIndex, newV);
					t = t.insert (newIndex, newT);
					v[v.size-1] = v[v.size-2];
					n.stop.reset; clock.clear; n.play(clock,0); r.play(clock,0);
				}, {
					v = v.insert (newIndex, newV);
					t = t.insert (newIndex, newT);
					v[v.size-1] = v[v.size-2];	
				});
			};
			
			maxValue = SCNumberBox( a.window, a.layDown(70, 20 ) )
				.value_(1.0)
				.action_( { |n| bus.set( \max, n.value ); } ); 
			
			minValue = SCNumberBox( a.window, a.layRight(70, 20 ) )
				.value_(0.0)
				.action_( { |n| bus.set( \min, n.value ); } ); 
				
			a.resizeToFit;
			a.front;
			b.resize_(1);
			
			r = Routine({ arg inval;
				var vNow, nowIndex, tNow, curV=0, curT=0;
				curV = v[0]; curT = t[0];
				loop {
					now = thisThread.beats;
					nowIndex = t.indexInBetween( now );
			
					vNow = [v.blendAt( nowIndex )] ++ v.copyRange( nowIndex.ceil.asInt, 9999);
					tNow = [t.blendAt( nowIndex )] ++ t.copyRange( nowIndex.ceil.asInt, 9999);
						tNow = tNow/maxT;
						tNow = tNow - tNow.first + 0.000000001;
					{
						b.thumbSize_( 2 );
						tNow.size.do({
							arg i;
							b.value_( [ tNow**0.5 , vNow] );
						//	b.setString( i, vNow[i].asString );
						});
					}.defer;
					0.1.yield;		
				}
			});
			
			clock = TempoClock.new;
			
			n = Routine ({ arg inval;	
				var now, time, index;
				now = thisThread.beats;
				index = t.indexInBetween( now ).ceil.asInt;
				time = t[index] - now;
				bus.set( \target, v[index], \fadeTime, time );
				time.yield;
				
				loop {
					time = t[index+1] - t[index];
					bus.set( \target, v[index+1], \fadeTime, time );
					index = index + 1;
					time.yield;
				}
			} );
			n.play(clock); r.play(clock);
		}, {
			"not a busEnvelope!".postln;
		});
	}

}