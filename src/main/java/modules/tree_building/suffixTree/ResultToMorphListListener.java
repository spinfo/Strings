package modules.tree_building.suffixTree;

/**
 * @author JR
 * @version 1.0
 */

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Stack;

import common.logicBits.ILogOp;
import modules.tree_building.suffixTree.Word;


public class ResultToMorphListListener  implements ITreeWalkerListener{
	
	// the suffix tree this will work on
	final BaseSuffixTree tree;
	boolean inverted = true;
	private Stack<Integer> nodeNrs = null;
	// the length of the path currently read
	private int lengthOfPath;
	
	
	
	
	public ArrayList <Word>words;
	
	int[] nodesWholePhrases;
	
	
	public ResultToMorphListListener(BaseSuffixTree suffixTree, boolean inverted) {
		this.tree = suffixTree;
		this.nodeNrs = new Stack<Integer>();
		
		this.inverted = inverted;
		this.nodesWholePhrases=new int[this.tree.nodes.length];
		
		this.words = new ArrayList<Word>();
		
	}
	
	@Override
	//	copy from ResultToFiniteStateMachineListener
	public void entryaction(int nodeNr, int level) throws IOException {
		this.nodeNrs.push(nodeNr);
		this.lengthOfPath = this.lengthOfPath + 
		this.tree.getNode(nodeNr).getEnd(0) - tree.getNode(nodeNr).getStart(0);
	
}

	@Override
	public void exitaction(int nodeNr, int level) throws IOException {
	// if the current node is a leaf of a whole input text, write out stack
		//System.out.println("exitAction entry nodeNr: "+nodeNr+" lengthOfPath: "+this.lengthOfPath);
		if (nodeIsLeafOfWholeInputText(nodeNr, this.lengthOfPath)) 
		
			{
			
			// mark node as in whole phrase;for paradigmatic relation;
			// except terminal nodes which don't branch but should be maintanined
			this.nodesWholePhrases[nodeNr]=3;//terminal node
			int anf, end, node;
			//if (this.inverted) System.out.println("exitAction inverted LeafNode: "+nodeNr);
			//else System.out.println("exitAction not inverted LeafNode: "+nodeNr);
			
			Word word=new Word();
			word.morphInWordList=new ArrayList<Integer>();
			this.words.add(word);
			
			if (this.inverted)
				
				{
				
				// 2-for loops, first loop for inverted order in word
				for (int i=0;i<this.nodeNrs.size();i++) 
					word.morphInWordList.add(this.nodeNrs.get(i));
				
				for (int i=this.nodeNrs.size()-1;i>=0;i-- )
				
					{ node=this.nodeNrs.get(i);
					
					  //System.out.println("exitAction node: "+node);
					  anf=tree.getNode(node).getStart(0);
					  end=tree.getNode(node).getEnd(0)-1;
					 
					 // for(int pos=end;pos>=anf;pos--)
					//	  if (this.tree.text[pos]!='$')
					//	  System.out.print(this.tree.text[pos]);
					//  System.out.print(" ");
					}
					
				
				}
			else {
				for (int i=0;i<this.nodeNrs.size();i++)
				
				{
					node=this.nodeNrs.get(i);
					word.morphInWordList.add(node);
					
					anf=tree.getNode(node).getStart(0);
					end=tree.getNode(node).getEnd(0);
					// for(int pos=anf;pos<end;pos++)
					//	  System.out.print(this.tree.text[pos]);
					//  System.out.print(" ");
					
				}
				
			};

				
		}
		this.lengthOfPath = this.lengthOfPath - 
				(tree.getNode(nodeNr).getEnd(0) - tree.getNode(nodeNr).getStart(0));
		this.nodeNrs.pop();
		
		// morphemes in contrast; mark mother if child is marked
		if (this.nodesWholePhrases[nodeNr]>0 )
			{
				//System.out.println("exitaction nodeNrs nodeNr: "+nodeNr);
				if(!this.nodeNrs.empty())
					{//System.out.println("peek: "+this.nodeNrs.peek() );
					if (this.nodesWholePhrases[this.nodeNrs.peek()]==0) {
					this.nodesWholePhrases[this.nodeNrs.peek()]=1;
					}
					else if (this.nodesWholePhrases[this.nodeNrs.peek()]==1) {
						this.nodesWholePhrases[this.nodeNrs.peek()]=2;
					}
				}
			
		}
	}
	
	// checks if the given node in this listeners tree corresponds to a whole
		// input text given the current path length.
		private boolean nodeIsLeafOfWholeInputText(int nodeNr, int pathLength) {
			Node node = this.tree.getNode(nodeNr);

			if (!node.isTerminal()) {
				return false;
			}
			//System.out.println("nodeIsLeafOfWholeInputText terminal node:"+nodeNr+
			//		" pathLength: "+pathLength);
			for (NodePosition position : node.getPositions()) {
				//System.out.println("nodeIsLeafOfWholeInputText position.getEnd: "+
				//		position.getEnd() +" tree.getTextBegin "+tree.getTextBegin(position.getTextNr())+
				//				" TextBegin+pathLength: "+(tree.getTextBegin(position.getTextNr()) + 
				//					pathLength));
				if (position.getEnd() == tree.getTextBegin(position.getTextNr()) + pathLength) {
					
					return true;
				}
			}

			return false;
		}
		
		
		public ArrayList<BranchedStringBufferElement> generateSortedBranchedStringList() {
			ArrayList <BranchedStringBufferElement>branchedStringElementList=new ArrayList <BranchedStringBufferElement>();
			BranchedStringBufferElement branchedStringElement;
			System.out.print("generateSortedBranchedStringList ");
			if(this.inverted) System.out.println("inverted");
			else System.out.println("normal");
			System.out.println();
			for (int i=0;i<this.words.size();i++){
				Word word=this.words.get(i);
				branchedStringElement=word.branchedString(this);
				branchedStringElementList.add(branchedStringElement);
				//System.out.println();
			}
			Collections.sort(branchedStringElementList,new StringBufferElementComparator());
			
			//printBranchedStringElementList(branchedStringElementList);
			return branchedStringElementList;
		}
		
		
		
		public StringBuffer resultBranchedStringElementList(ArrayList<ExtendedBranchedStringBufferElement>branchedStringElementList)
		{
			System.out.println("resultBranchedStringElementList");
			int nrInserts=0,nrInsertsLeftRight=0,nrInsertsRightLeft=0;
			StringBuffer outputBuffer= new StringBuffer();
			char splitSign;
			for(ExtendedBranchedStringBufferElement b:branchedStringElementList){
				 nrInserts=0;
				 //nrInsertsLeftRight=0;nrInsertsRightLeft=0;
				//System.out.println(b.stringBuffer);
				// result is string representation of logical operation ('and'  or 'or')
				StringBuffer result=new StringBuffer(b.stringBuffer);
				// leftRight is element derived from suffix tree with normal (left right) input text
				//StringBuffer leftRight=new StringBuffer(b.stringBuffer);
				// rightLeft is element derived from suffix tree with inverted (right left) input text
				//StringBuffer rightLeft=new StringBuffer(b.stringBuffer);
				
				for (int i=0;i<b.bitSet.length();i++){
					if( b.bitSet.get(i)) {
						if (b.leftRightBitSet.get(i)) {
							if (b.rightLeftBitSet.get(i)) splitSign='|';
							else splitSign='>';
						} else {splitSign='<';};
						result.insert(i+nrInserts,splitSign);
						nrInserts++;
					}
									
				}
				
				
				// int nrInserts=0;
				// pipe sign | as separator; if pipe sign is inserted in string,
				// insert must be counted (by nrInserts) to put pipe in correct position
								
				/*for (int i=0;i<b.bitSet.length();i++){
					if(b.bitSet.get(i)) {
						System.out.print('|');
						outSbElement.insert(i+nrInserts,'|');
						nrInserts++;
						}
						//else 
						System.out.print(' ');
					
				} //for int i=0;i<b.bitSet.length();i++)
				System.out.println();//System.out.println();
				System.out.println(outSbElement);
				*/
				outputBuffer.append(result.append(System.getProperty("line.separator")));
			}
			 return outputBuffer;
						
		}
		
		public ArrayList<ExtendedBranchedStringBufferElement>logOp(ArrayList<BranchedStringBufferElement>l1,
				ArrayList<BranchedStringBufferElement>l2, ILogOp il ) {
			// necessary precondition in1 and in2 contain identical strings
			// ??toDo throw eception if not??
			ArrayList<ExtendedBranchedStringBufferElement>resList=new ArrayList<ExtendedBranchedStringBufferElement>();
			for (int i=0;i<l1.size();i++){
				// to check: new string buffer, newElement is clone of
				// element from l1 List!!!
				// HINT TODO??: might be useful to extend class BranchedStringBufferElement to
				// a class BranchedStringBufferBitSourcesElement with two further bitsets, one from
				// l1.get(i).bitSet and the second from l2.get(i).bitSet)
				// with further information for Distance seq.
				BitSet resOp=il.logOperation(l1.get(i).bitSet,l2.get(i).bitSet);
				ExtendedBranchedStringBufferElement newElement=
				new ExtendedBranchedStringBufferElement(l1.get(i).stringBuffer,resOp,
				l1.get(i).bitSet,l2.get(i).bitSet);
				//newElement.firstBitSet=l1.get(i).bitSet;
				//newElement.secondBitSet=l2.get(i).bitSet;
				resList.add(newElement);
				
			}
			
			
			
			return resList;
			
		}
		
	
		
}
	
	


