GroupManager : Environment {
	classvar <>managers;
	var server, 
		know=true, isPlaying=false,
		<insertTarget, <>insertAction
	;
	
	*initClass {
		managers = IdentityDictionary.new;
	}
	
	*get { 
		| server |
		var newTmp;
		server = server ? Server.default;
		^( managers.atFail(server, { managers.put( server, newTmp = this.new( server ) ); newTmp }) )
	}
			
	*new {
		| server |
		^super.new.init( server )
	}
	
	*newStructure {
		| server, dict |
		^super.new.init( server ).addFromArray( dict )
	}
	
	init {
		| s |
		server = s;
		this.know = true;
//		this.put( \head, server.asTarget );
//		insertTarget = server.asTarget;
		insertAction = \tail;
	}
	
	insertTarget_{
		| target |
		if( target.isSymbol, {
			insertTarget = this.at( target )
		},{
			if( this.values.indexOf( target ).notNil, {
				insertTarget = target
			})
		})
	}
	
	addFromArray {
		| array, target |
		var lastGroup, name, children;
		target = target ? insertTarget;
		array.do({
			| val, i |
			name = if( val.class == Association, { val.key }, { val } );
			if( i==0, {
				lastGroup = this.prNewGroup( name, target, \addToTail );
			},{			
				lastGroup = this.prNewGroup( name, lastGroup, \addAfter );
			});
			if( val.class == Association, { this.addFromArray( val.value, lastGroup ) })
		})
	}
	
	at {
		| key |	
		^super.at(key) ?? { ^this.prNewGroup( key ) }
	}
	
	newGroup { | key, target, action | ^this.prNewGroup( key, target, action ) }
	
	prNewGroup {
		| key, target, action |
		var group;
		target = target ? insertTarget;
		action = action ? insertAction;
		group = Group(addAction:action, target:target);
		group.register;
		this.put( key, group );
		^group
	}
	
	put {
		| key, val ... args |
		if (isPlaying)
			{ this.prPlayGroup( val ) };
		super.put( key, val)
	}
	
	play {
		server.makeBundle( nil, {
			this.values.do( this.prPlayGroup( _ ) );
		});
		isPlaying = true;
	}
	
	prPlayGroup {
		| group |
		if( group.isPlaying.not, {
			if( group.target.notNil and: { group.target.isGroupPlaying.not }, 
				{ this.prPlayGroup( group.target ) });
			group.play
		})	
	}
	
	free {
		this.values.do( _.deepFree );
		isPlaying = false;
	}
}	
