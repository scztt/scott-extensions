TimelineNode { 
	var <>obj, <>time, <>prev, <>next;
	
	*new { arg item, time;
		^super.newCopyArgs(item, time)
	}
	
	remove {
		if ( prev.notNil, { prev.next_(next); });
		if ( next.notNil, { next.prev_(prev); });
		next = prev = nil;
	}
	
	asString {
		^format("%:(%)", time, obj )
	}
}

Timeline {
	var <head, <tail, <size=0, partitions;
	var partitionScale=5;
	
	*new { 
		^super.new.init();
	}
	
	init {
		this.add( \beginning, 0 );
		//size = 1;
	}
	
	species { ^this.class }
	
	add { arg obj, time;
		var node = TimelineNode.new(obj, time);
		this.addNode( node );
	}
	
	addNode {
		| node |
		var nodeAfter, slot;
		if( partitions.isNil, { partitions = Array.newClear( 1000 ) } );
		slot = (node.time/partitionScale).floor;
		
		nodeAfter = this.nodeAfterTime( node.time );
		if( nodeAfter.notNil, {
			this.addBeforeNode( nodeAfter, node )
		},{
			if( tail.notNil, {
				this.addAfterNode( tail, node )
			},{
				head = tail = node
			})
		});
		this.changed( \addedNode, node );
	}
	
	remove {
		| node |
		if( head == node, { head = node.next });
		if( tail == node, { tail = node.prev });
		^node.remove
	}
	
	nodeAfterTime {
		| time, startHere |
		var node = startHere ? head;
		while( { node.notNil and: {(time >= node.time)} }, {
			node = node.next;
		});
		^node
	}

	nodeBeforeTime {
		| time, startHere |
		var node = startHere ? tail;
		while( { node.notNil and: {(time <= node.time)} }, {
			node = node.prev;
		});
		^node
	}
	
	addAfterNode { 
		| targetNode, addNode |
		if( targetNode.next.notNil, {
			targetNode.next.prev = addNode;
		});
		addNode.prev = targetNode;
		targetNode.next = addNode;
		if( targetNode == tail, { tail = addNode });
	}
	
	addBeforeNode {
		| targetNode, addNode |
		if( targetNode.prev.notNil, {
			targetNode.prev.next = addNode;
			addNode.prev = targetNode.prev;
		});
		addNode.next = targetNode;
		targetNode.prev = addNode;
		if( targetNode == head, { head = addNode });
	}
		
	asStream {
		var node = head;
		^Routine({ 
			loop {
				node.yield; 
				if( node.notNil, { node = node.next });
			}
		})
	}
	
	asString {
		var str = "";
		var current = head;
		if( head.notNil, {
			while( {current.notNil},  {
				str = str.catArgs( format( "%: %\n", current.time, current.obj.asCompileString ) );
				current = current.next;
			})
		});
		^format( "Timeline [\n%]", str )
	}
}

TimelinePlayer { 
	var <timeline, clock, <currentNode;
	var <next_, <direction=1;
	var <>playing=true, <queue;
	var <scheduler, startTime, skipLateEvents=false, resetToTime;
	
	*new {
		| timeline, clock |
		^super.newCopyArgs(timeline, clock).init
	}
 
	init {
		playing = true;
		currentNode = timeline.head;
		queue = List.new();
		scheduler = TempoClock();
		//startTime = scheduler.beats;
		this.scheduleNext();
	}
	
	play {
		//startTime = scheduler.beats;
		this.scheduleNext();
		playing=true;
	}
	
	pause {
		scheduler.clear;
		playing=false;		
	}
	
	tempo {
		^scheduler.tempo
	}
	
	
	tempo_{
		|t|
 		scheduler.tempo = t;
	}
		
	direction_{
		| newDir |
		if( (newDir*direction).isNegative, {
			if( currentNode.isNil, {
				( newDir>0 ).if(
					{ currentNode = timeline.head },
					{ currentNode = timeline.tail })
			},{
				// ick, don't want to worry about this right now
			})
		})
	}
	
	playQueue {
		queue.do({ 
			|item| 
			item.obj.use({item.obj[\play].value()})   
		});
		queue.clear
	}
	
	liveAdd {
		| node |
		"liveadd: % at % \n".postf( node.obj, node.time );
		// This means, we've got no nodes in the future at all.
		if( currentNode.isNil, { 
			timeline.addNode( node );
			//currentNode = timeline.head;
			scheduler.schedAbs( currentNode.time, { this.doNext });
			"scheduled % at % \n".postf(currentNode.obj, currentNode.time);
		},{
			if( node.time > scheduler.beats && 
				( currentNode.next.isNil or: { node.time < currentNode.next.time } ), {
					timeline.addNode( node );
					"trying to resched".postln;
					scheduler.clear();
					this.scheduleNext();
			},{
				timeline.addNode( node );
			});	
		});
		
		this.postNodeOrder();
	}
	
	liveReschedule {
		| node, time |
		var currentNodeNext;
		currentNodeNext = currentNode.next;
		
		node.time = time;
		timeline.remove( node );
		timeline.addNode( node );
		
		case
		{ node == currentNode }
			{
			currentNode = timeline.nodeBeforeTime( scheduler.beats, currentNodeNext ? timeline.tail );
			}
			
		{ node == currentNodeNext }
			{		
			scheduler.clear();
			this.scheduleNext();
			}
					
		^node
	}
	
	liveRemove {
		| node |
		var currentNodeNext;
		currentNodeNext = currentNode.next;

		timeline.remove( node );
		
		case
		{ node == currentNode }
			{
			currentNode = timeline.nodeBeforeTime( scheduler.beats, currentNodeNext ? timeline.tail );
			}
			
		{ node == currentNodeNext }
			{		
			scheduler.clear();
			this.scheduleNext();
			}
		
		^node
	}
	
	postNodeOrder {
		var n;
		"Node order:".postln;
		n = timeline.head;
		while({ n.notNil }, {
			if( n == currentNode, { 
				"[%]\n".postf( n.time ) 
			},{
				n.time.postln;
			});
			n = n.next;
		});
		"\n".postln;
	}
	
	scheduleNext {
		if( currentNode.notNil, {
			if( direction.isPositive, {
				if( currentNode.next.notNil, {
					"next node at: %, current time: %".postf( currentNode.next.time, scheduler.beats );
					if( currentNode.next.time > (scheduler.beats), {
						scheduler.schedAbs( currentNode.next.time, { this.doNext } );
						"scheduled % at % . Current time: %\n".postf(currentNode.next.obj, currentNode.next.time, scheduler.beats);
					})
				})
				
			},{
				// ??
			})
		})
	}
	
	doNext {
		if( currentNode.notNil, {		
			while( { currentNode.next.notNil and: { currentNode.next.time <= scheduler.beats }  }, { 
				currentNode = currentNode.next;
				currentNode.obj.use({      currentNode.obj[\play].value     });
			});
			
			if( resetToTime.notNil, { 
				this.prReset();
			},{
				this.scheduleNext();
			});
			
		})
	}
	
	time {
//		^scheduler.beats - startTime;
		^scheduler.beats;
	}
	
	reset {
		| time=0 |
		("resetting to " + time).postln;
		resetToTime = time;
	}
	
	prReset {
		"prReset".postln;
		scheduler.clear;
		scheduler.init(scheduler.tempo, 0, AppClock.seconds-resetToTime);
		resetToTime = nil;
		
		currentNode = timeline.head;		
		queue = List.new();
		scheduler.schedAbs( 0, {this.scheduleNext()} );
	}
}