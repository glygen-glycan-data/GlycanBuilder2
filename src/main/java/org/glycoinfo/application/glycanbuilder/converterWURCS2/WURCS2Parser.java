package org.glycoinfo.application.glycanbuilder.converterWURCS2;

import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.converter.GlycanParser;
import org.eurocarbdb.application.glycanbuilder.logutility.LogUtils;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.BBoxManager;
import org.glycoinfo.GlycanFormatconverter.io.WURCS.WURCSImporter;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.application.glycanbuilder.util.exchange.WURCSToGlycanException;
import org.glycoinfo.application.glycanbuilder.util.exchange.exporter.GlycanToWURCSGraph;
import org.glycoinfo.application.glycanbuilder.util.exchange.importer.WURCSSequence2ToGlycan;
import org.glycoinfo.application.glycanbuilder.util.exchange.importer.glycontainer2glycan.GlyContainer2Glycan;

public class WURCS2Parser implements GlycanParser{
	
	public void setTolerateUnknown(boolean f) {}
	
	public String writeGlycan(Glycan structure) {
		if (structure.isFragment()) return "";
    if (structure.isComposition()) return "";

		try{
			LinkageTypeOptimizer linkOpt = new LinkageTypeOptimizer();
			linkOpt.start(structure);

			GlycanToWURCSGraph glycan2graph = new GlycanToWURCSGraph();
			glycan2graph.start(structure);
			WURCSFactory wf = new WURCSFactory(glycan2graph.getGraph());
			
			return wf.getWURCS();
		}catch (Exception e) {
			LogUtils.report(e);
			return "";
		}
	}
	
	public Glycan readGlycan(String str, MassOptions mass_opt) throws Exception{
		if(str.equals("") || !str.contains("WURCS")) throw new WURCSToGlycanException(str + " is wrong format");
		mass_opt.setDerivatization("Und");
		mass_opt.ION_CLOUD.set("Na", 0);
		
		str = str.trim();		
		if(str.contains("\t")) str = str.substring(str.indexOf("\t") + 1);
		
		WURCSFactory wf = new WURCSFactory(str);
		Glycan glycan;
		try {
			WURCSSequence2ToGlycan seq22glycan = new WURCSSequence2ToGlycan();
			seq22glycan.start(wf, mass_opt);
			glycan = seq22glycan.getGlycan();
		} catch (WURCSToGlycanException ex) {
			throw ex;
		} catch (Exception undescribed) {
			undescribed.printStackTrace();
			throw new WURCSToGlycanException("could not convert this WURCS to a structure ("
					+ undescribed.getClass().getSimpleName()
					+ (undescribed.getMessage() != null ? ": " + undescribed.getMessage() : "")
					+ "): " + shortenedForError(str), undescribed);
		}
		refuseCycles(glycan.getRoot(), java.util.Collections.newSetFromMap(
				new java.util.IdentityHashMap<Residue, Boolean>()));
		return glycan;

		//WURCSImporter wi = new WURCSImporter();
		//GlyContainer2Glycan gc2g = new GlyContainer2Glycan();
		//Glycan ret = gc2g.start(wi.start(str), mass_opt);
		//return ret;
	}

	private static String shortenedForError(String sequence) {
		String flat = sequence.strip();

		return flat.length() > 80 ? flat.substring(0, 80) + "..." : flat;
	}

	private static void refuseCycles(Residue residue, java.util.Set<Residue> visited) throws Exception {
		if (residue == null) return;
		if (!visited.add(residue))
			throw new WURCSToGlycanException("this WURCS makes two connections between the same residues"
					+ " (a ring through a bridge), which cannot be represented yet");

		for (org.eurocarbdb.application.glycanbuilder.linkage.Linkage linkage : residue.getChildrenLinkages())
			refuseCycles(linkage.getChildResidue(), visited);
	}

	@Override
	public String writeGlycan(Glycan structure, BBoxManager bboxManager) {
		throw new UnsupportedOperationException();
	}
}
