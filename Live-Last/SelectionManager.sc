SelectionManager {
	classvar selectionManagers;
	var <selection, selectionOrder;
	
	*new {
		| allowMultiSelect=true |
		^super.new.init( allowMultiSelect );
	}
	
	init {
		selection = List.new;
	}
	
	setSelection {
		| item |
		this.clearSelection();
		item.select();
		selection = IdentitySet.with(item);
		this.changed(\selection);
	}
	
	addSelection {
		| ...items |
		selection = selection.addAll( items );
		items.do( _.select() );
		this.changed(\selection);
	}
	
	removeSelection {
		| ...items |
		items.do({
			| item |
			item.deselect();
		});
		selection.removeAll( items );
		this.changed(\selection);	
	}
	
	clearSelection {
		selection.do({
			| item |
			item.deselect();
		});
		selection.clear;
		this.changed(\selection);	
	}
}