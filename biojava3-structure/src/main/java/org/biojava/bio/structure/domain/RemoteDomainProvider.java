package org.biojava.bio.structure.domain;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;


import org.biojava.bio.structure.align.ce.AbstractUserArgumentProcessor;
import org.biojava.bio.structure.align.client.StructureName;


import org.biojava.bio.structure.scop.ScopDatabase;
import org.biojava.bio.structure.scop.ScopDomain;
import org.biojava.bio.structure.scop.ScopFactory;


public class RemoteDomainProvider extends SerializableCache<String,SortedSet<String>> implements DomainProvider{

	public String url = RemotePDPProvider.DEFAULT_SERVER;

	ScopDatabase scop;
	RemotePDPProvider pdp;

	private static String CACHE_FILE_NAME = "remotedomaincache.ser";

	
	public RemoteDomainProvider(){
		this(false);
	}

	/** initialize this provider with caching enabled
	 * 
	 * @param cache
	 */
	public RemoteDomainProvider(boolean cache){
		super(CACHE_FILE_NAME);
		
		if( ! cache)
			disableCache();
		
		scop = ScopFactory.getSCOP();
		pdp = new RemotePDPProvider(true);

		
	}


	@Override
	public SortedSet<String> getDomainNames(String name) {


		if ( name.length() < 4)
			throw new IllegalArgumentException("Can't interpret IDs that are shorter than 4 residues!");

		if ( serializedCache != null){
			if ( serializedCache.containsKey(name)){
				return serializedCache.get(name);
			}
		}

		StructureName n = new StructureName(name);

		List<ScopDomain>scopDomains = scop.getDomainsForPDB(n.getPdbId());

		String chainID = n.getChainId();

		if ( scopDomains == null || scopDomains.size() == 0){
			SortedSet<String> data= getPDPDomains(n);
			cache(name,data);
			return data;
		} else {
			SortedSet<String> r = new TreeSet<String>();
			for ( ScopDomain d: scopDomains){
				StructureName s = new StructureName(d.getScopId());

				if( chainID == null){
					r.add(s.getName());

				} else if( s.getChainId().equalsIgnoreCase(n.getChainId())) {
					// SCOP IDS are case insensitive...
					r.add(s.getName());
				}
			}
			cache(name,r);
			return r;
		}



	}

	


	private SortedSet<String> getPDPDomains(StructureName n) {
		SortedSet<String> pdpDomains = pdp.getPDPDomainNamesForPDB(n.getPdbId());

		SortedSet<String> r = new TreeSet<String>();
		String chainID = n.getChainId();
		for ( String s : pdpDomains){
			StructureName d = new StructureName(s);
			if ( chainID == null)
				r.add(s);
			else if ( d.getChainId().equals(n.getChainId())){
				r.add(s);
			}
		}
		System.out.println(n + " got PDP domains: "+ r);
		return r;
	}

	public static void main(String[] args){
		System.setProperty(AbstractUserArgumentProcessor.PDB_DIR,"/Users/andreas/WORK/PDB");
		
		String name ="3KIH.A";
		try {
			RemoteDomainProvider me = new RemoteDomainProvider(true);
			System.out.println(me.getDomainNames(name));
			StructureName n = new StructureName(name);
			System.out.println(n);
			//System.out.println(new  AtomCache().getStructure(name));
			me.flushCache();
		} catch (Exception e){
			e.printStackTrace();
		}


	}

	@Override
	public void flushCache() {
		super.flushCache();
		pdp.flushCache();
	}
	
	


}
