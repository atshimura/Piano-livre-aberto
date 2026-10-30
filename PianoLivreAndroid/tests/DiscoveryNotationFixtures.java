package br.org.pianolivre;
import java.util.*;
import java.nio.charset.StandardCharsets;
public final class DiscoveryNotationFixtures {
 public static void main(String[] args){
  for(int[] notes:new int[][]{{},{60},{64},{67},{60,64,67},{36},{84},{0},{127},{64},{55,59,60,62},{21},{108}}){
   ArrayList<Integer> list=new ArrayList<>();for(int n:notes)list.add(n);
   System.out.println(Base64.getEncoder().encodeToString(DiscoveryScoreBuilder.build(list,notes.length==4).getBytes(StandardCharsets.UTF_8)));
  }
 }
}
