+ ProxySpace {

	printGroupOrder {
		var sortedByGroup, envir;
		envir = this.envir;
		sortedByGroup = envir.keys.asList.sort( { |a, b| envir.at(a).group.nodeID < envir.at(b).group.nodeID } );
		sortedByGroup.do({ 
			|item| 
			envir.at(item).group.nodeID.post;
			" -> ~".post;
			item.asString.postln;
		})
		^"";
	}

}