+Model {
	signal { arg what ... moreArgs;
		dependants.do({ arg item;
			item.update(this, what, *moreArgs);
		});
	}
	connect { arg dependant;
		if (dependants.isNil, {
			dependants = IdentitySet.new(4);
		});
		dependants.add(dependant);
	}
	disconnect { arg dependant;
		if (dependants.notNil, {
			dependants.remove(dependant);
		});
	}
}