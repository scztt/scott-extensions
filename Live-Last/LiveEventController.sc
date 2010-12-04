LiveEventController : LinkedObjectList {
	*new {
		^super.new;
	}
	
	add {
		| item |
		item.addDependant(this);
		^super.add(item);
	}
	
	insertAllAfter {
		| position, list |
		list.do({
			| item |
			item.addDependant(this);
		});
		^super.insertAllAfter( min(position, this.size), list );
	}
	
	insert {
		| position, item |
		item.addDependant(this);
		^super.insert( position, item );
	}
	
	moveTo {
		| position, item |
		^super.moveTo(position, item);
	}
	
	remove {
		| item |
		item.removeDependant(this);
		^super.remove(item);
	}
	
	update {
		| who, what ...args |
		
		switch (what)
		{ \childRemoved }
			{
				"removing child".postln;
				this.remove(who);
				this.changed(\itemsRemoved, [who]);
			}
		;
	}
}
