/*
*   EuroCarbDB, a framework for carbohydrate bioinformatics
*
*   Copyright (c) 2006-2009, Eurocarb project, or third-party contributors as
*   indicated by the @author tags or express copyright attribution
*   statements applied by the authors.  
*
*   This copyrighted material is made available to anyone wishing to use, modify,
*   copy, or redistribute it subject to the terms and conditions of the GNU
*   Lesser General Public License, as published by the Free Software Foundation.
*   A copy of this license accompanies this distribution in the file LICENSE.txt.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
*   or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
*   for more details.
*
*   Last commit: $Rev$ by $Author$ on $Date::             $  
*/
/**
   @author Alessio Ceroni (a.ceroni@imperial.ac.uk)
*/

package org.eurocarbdb.application.glycanbuilder.renderutil;

import java.util.*;
import java.awt.Rectangle;

import org.w3c.dom.Element;

import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.application.glycanbuilder.util.GlycanUtils;
import org.eurocarbdb.application.glycanbuilder.DefaultPaintable;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.Residue;
import org.eurocarbdb.application.glycanbuilder.linkage.Linkage;

class SVGGlycanRenderer extends GlycanRendererAWT {

    Glycan theStructure=null;
    Integer lastResidueIndex=0;
    HashMap<Object,Integer> residueIndex = new HashMap<Object,Integer>();
    HashMap<Object,String> residueUndetQuantity = new HashMap<Object,String>();
    HashMap<Object,String> residueUndetParentPos = new HashMap<Object,String>();
    HashMap<Object,String> residueUndetChildPos = new HashMap<Object,String>();
    Residue root=null;

    public SVGGlycanRenderer(GlycanRendererAWT src) {
        theResidueRenderer = src.theResidueRenderer;
        theLinkageRenderer = src.theLinkageRenderer;
        theResiduePlacementDictionary = src.theResiduePlacementDictionary;
        theResidueStyleDictionary = src.theResidueStyleDictionary;
        theLinkageStyleDictionary = src.theLinkageStyleDictionary;
        theGraphicOptions = src.theGraphicOptions;
    }

    public int getNodeID(Object node) {
        if( node==null ) return -1;
        if( residueIndex.containsKey(node) ) return residueIndex.get(node);
        lastResidueIndex += 1;
        residueIndex.put(node,lastResidueIndex);
        return lastResidueIndex;
    }

    public void paint(GroupingSVGGraphics2D g2d, Glycan structure, HashSet<Residue> selected_residues, HashSet<Linkage> selected_linkages, boolean show_mass, boolean show_redend, PositionManager posManager, BBoxManager bboxManager) {
        if (structure == null || structure.getRoot(show_redend) == null)
            return;

	theStructure = structure;

        boolean isAlditol = show_redend;
        if(!structure.isComposition()) {
            isAlditol = GlycanUtils.isShowRedEnd(structure, theGraphicOptions, show_redend);
        }

        this.assignID(structure);

        selected_residues = (selected_residues != null) ? selected_residues : new HashSet<>();
        selected_linkages = (selected_linkages != null) ? selected_linkages : new HashSet<>();

        // draw core structures
        if (!structure.isComposition()) {
            paintResidue(g2d, structure.getRoot(isAlditol), selected_residues, selected_linkages, null, posManager, bboxManager);
        }

        // draw fragments
        paintBracket(g2d, structure, selected_residues, selected_linkages, null, posManager, bboxManager);

        if(theGraphicOptions.NOTATION.equals(GraphicOptions.NOTATION_SNFG)) {
            g2d.addGroup("legend", structure, null);
            displayLegend(new DefaultPaintable(g2d), structure, show_redend, bboxManager);
        }
        if (show_mass) {
            g2d.addGroup("m", structure, null);
            displayMass(new DefaultPaintable(g2d), structure, show_redend, bboxManager);
        }
    }

	public void paintResidue(GroupingSVGGraphics2D g2d, Residue node, 
				 HashSet<Residue> selected_residues, 
				 HashSet<Linkage> selected_linkages, 
				 Collection<Residue> active_residues, PositionManager posManager, 
				 BBoxManager bboxManager) {    
		if (node == null) return;

		Rectangle parent_bbox = bboxManager.getParent(node);
		Rectangle node_bbox = bboxManager.getCurrent(node);
		Rectangle border_bbox = bboxManager.getBorder(node);
		Rectangle support_bbox = bboxManager.getSupport(node);

		// not shown 
		if (node_bbox == null) return;

		// paint edges
		for (Linkage link : node.getChildrenLinkages()) {

			Residue child = link.getChildResidue();
			Rectangle child_bbox = bboxManager.getCurrent(child);
			Rectangle child_border_bbox = bboxManager.getBorder(child);

			if (child_bbox != null && !posManager.isOnBorder(child)) {
			        Element g = g2d.addGroup("l",theStructure,node,child);
				if (node.isSaccharide()) {
				    g.setAttribute("data.type","Linkage");
				    g.setAttribute("data.parentResidueIndex",Integer.toString(getNodeID(node)));
				    g.setAttribute("data.parentPositions",link.getParentPositionsString());
				    // g.setAttribute("data.parentLinkageType",link.getParentLinkageType().toString());
				    g.setAttribute("data.childResidueIndex",Integer.toString(getNodeID(child)));
				    g.setAttribute("data.childPositions",link.getChildPositionsString());
				    // g.setAttribute("data.childLinkageType",link.getChildLinkageType().toString());
				} else if (node.isReducingEnd() || node.isFreeReducingEnd()) {
                                    root = child;
                                }
				boolean selected = (selected_residues.contains(node) && selected_residues.contains(child)) || selected_linkages.contains(link);
				boolean active = (active_residues == null || (active_residues.contains(node) && active_residues.contains(child)));
				theLinkageRenderer.paintEdge(new DefaultPaintable(g2d),link,selected,node_bbox,border_bbox,child_bbox,child_border_bbox);                
			}        
		}

		// paint node
		Element g = g2d.addGroup("r",theStructure,node);
		if (node.isSaccharide()) {
		    g.setAttribute("data.type","Monosaccharide");    
		    g.setAttribute("data.residueIndex",Integer.toString(getNodeID(node)));
		    g.setAttribute("data.residueName",node.getResidueName());
		    g.setAttribute("data.residueRingSize",""+node.getRingSize());
		    g.setAttribute("data.residueChirality",""+node.getChirality());
		    g.setAttribute("data.residueAnomericState",""+node.getAnomericState());
                    if (node.isAlditol()) {
		      g.setAttribute("data.residueIsAlditol","true");
                    }
                    if (residueUndetQuantity.containsKey(node)) {
                      g.setAttribute("data.residueUndeterminedMultiplicity",residueUndetQuantity.get(node));
                      g.setAttribute("data.residueUndeterminedParentPos",residueUndetParentPos.get(node));
                      g.setAttribute("data.residueUndeterminedChildPos",residueUndetChildPos.get(node));
                    }
                    if (node.isFreeReducingEnd() || node.isReducingEnd() || node == root) {
		      g.setAttribute("data.residueIsReducingEnd","true");    
                    }
		} else if (node.isSubstituent()) {
		    g.setAttribute("data.type","Substituent");    
		    g.setAttribute("data.residueName",node.getResidueName());
                    if (residueUndetQuantity.containsKey(node)) {
		      g.setAttribute("data.residueIndex",Integer.toString(getNodeID(node)));
                      g.setAttribute("data.residueUndeterminedMultiplicity",residueUndetQuantity.get(node));
                      g.setAttribute("data.residueUndeterminedParentPos",residueUndetParentPos.get(node));
                      g.setAttribute("data.residueUndeterminedChildPos",residueUndetChildPos.get(node));
                    } else {
                      Linkage acceptorLinkage = node.getParentLinkage();
                      g.setAttribute("data.parentResidueIndex",Integer.toString(getNodeID(acceptorLinkage.getParentResidue())));
                      g.setAttribute("data.parentPositions",acceptorLinkage.getParentPositionsString());
                      g.setAttribute("data.childPositions",acceptorLinkage.getChildPositionsString());
                    }
                }

		boolean selected = selected_residues.contains(node);
		boolean active = (active_residues == null || active_residues.contains(node));
		theResidueRenderer.paint(new DefaultPaintable(g2d), node, selected, active, posManager.isOnBorder(node), parent_bbox, node_bbox,
				support_bbox,posManager.getOrientation(node));

		// paint children
		for (Linkage link : node.getChildrenLinkages())
			paintResidue(g2d, link.getChildResidue(), selected_residues, selected_linkages, active_residues, posManager, bboxManager);

		// paint info
		for (Linkage link : node.getChildrenLinkages()) {

			Residue child = link.getChildResidue();
			Rectangle child_bbox = bboxManager.getCurrent(child);
			Rectangle child_border_bbox = bboxManager.getBorder(child);

			if (child_bbox != null && !posManager.isOnBorder(child)) {
				g2d.addGroup("li",theStructure,node,child);
				theLinkageRenderer.paintInfo(new DefaultPaintable(g2d),link,node_bbox,border_bbox,child_bbox,child_border_bbox);                        
			}
		}
	}


	public void paintBracket(GroupingSVGGraphics2D g2d, Glycan _glycan, 
				 HashSet<Residue> selected_residues, 
				 HashSet<Linkage> selected_linkages, 
				 Collection<Residue> active_residues, PositionManager posManager, 
				 BBoxManager bboxManager) {    

		if(_glycan == null || _glycan.getBracket() == null) return;

		Residue bracket = _glycan.getBracket();
		Rectangle parent_bbox = bboxManager.getParent(bracket);
		Rectangle bracket_bbox = bboxManager.getCurrent(bracket);
		Rectangle support_bbox = bboxManager.getSupport(bracket);

		// paint bracket
		g2d.addGroup("b",theStructure,bracket);
		boolean selected = selected_residues.contains(bracket);
		boolean active = (active_residues == null || active_residues.contains(bracket));

		if(!_glycan.isComposition())
			theResidueRenderer.paint(new DefaultPaintable(g2d), bracket, selected, active, false,
						 parent_bbox, bracket_bbox, support_bbox, posManager.getOrientation(bracket));

		// paint antennae
		for (Linkage link : bracket.getChildrenLinkages()) {
			Residue child = link.getChildResidue();

			if (child.getType().getDescription().equals("no glycosidic linkages")) continue;

			int quantity = bboxManager.getLinkedResidues(child).size() + 1;

			Rectangle node_bbox = bboxManager.getParent(child);
			Rectangle child_bbox = bboxManager.getCurrent(child);
			Rectangle child_border_bbox = bboxManager.getBorder(child);

			if (child_bbox != null) {
				// paint edge
				if (!posManager.isOnBorder(child)) {
					g2d.addGroup("l",theStructure,bracket,child);
					selected = (selected_residues.contains(bracket) && selected_residues.contains(child)) || selected_linkages.contains(link);
					active = (active_residues == null || (active_residues.contains(bracket) && active_residues.contains(child)));
					if (!_glycan.isComposition()) {
						theLinkageRenderer.paintEdge(new DefaultPaintable(g2d), link, selected, node_bbox, node_bbox, child_bbox, child_border_bbox);
					}
				}

				// paint child
                                residueUndetQuantity.put(child,Integer.toString(quantity));
                                residueUndetParentPos.put(child,link.getParentPositionsString());
                                residueUndetChildPos.put(child,link.getChildPositionsString());
				paintResidue(g2d, child, selected_residues, selected_linkages, active_residues, posManager,bboxManager);    

				// paint info
				if (!posManager.isOnBorder(child)) {
					if (_glycan.isComposition()) {
						node_bbox.x = node_bbox.x + theGraphicOptions.NODE_SPACE;
					}
					g2d.addGroup("li",theStructure,bracket,child);
					theLinkageRenderer.paintInfo(new DefaultPaintable(g2d), link, node_bbox, node_bbox, child_bbox, child_border_bbox);
				}

				// paint quantity
				if (quantity > 1)
					paintQuantity(new DefaultPaintable(g2d), child, quantity, bboxManager);        
			}
		}
	}

}
