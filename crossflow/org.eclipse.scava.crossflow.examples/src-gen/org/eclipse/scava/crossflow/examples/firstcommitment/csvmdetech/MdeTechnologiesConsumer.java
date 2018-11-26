package org.eclipse.scava.crossflow.examples.firstcommitment.csvmdetech;

public interface MdeTechnologiesConsumer {

	public void consumeMdeTechnologies(ExtensionKeywordTuple extensionKeywordTuple);
	
	/**
	 * wraps consumeMdeTechnologies() to provide task status information
	 */
	public void consumeMdeTechnologiesActual(ExtensionKeywordTuple extensionKeywordTuple);

}