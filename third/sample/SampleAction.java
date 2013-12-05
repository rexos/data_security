/*
 *
 * Copyright (c) 2000, 2002, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * -Redistributions of source code must retain the above copyright
 * notice, this  list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduct the above copyright
 * notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the name of Oracle nor the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
 * DAMAGES OR LIABILITIES  SUFFERED BY LICENSEE AS A RESULT OF  OR
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THE SOFTWARE OR
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that Software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */

package sample;

import java.util.*;
import java.io.*;
import java.security.PrivilegedAction;
import javax.security.auth.Subject;
import java.security.Principal;
import java.security.AccessController;
import sample.principal.SamplePrincipal;
/**
 * <p> This is a Sample PrivilegedAction implementation, designed to be
 * used with the Sample application.
 *
 */
public class SampleAction implements PrivilegedAction {


    /**
     * searchMovie looks for the movie named "filename" into
     * the directories specified by the "subDir" String
     */
    private String searchMovie( String subDir, String filename){
	String directories[] = subDir.split(" ");
	filename = filename + ".txt";
	for( int i = 0; i < directories.length; i++ ){
	    if( !directories[i].equals("Admin") ){ // we do not have a directory called "Admin" so don't look for it.
		File dir = new File("sample/db/"+directories[i]+"/");
		File movies[] = dir.listFiles();
		for( int j = 0; j < movies.length; j++ ){
		    if( (movies[j].getName()).equals(filename) ){
			return movies[j].getAbsolutePath();
		    }
		}
	    }
	}
	return "";
    }
    

    /**
     * This method is called to prompt the user and read the 
     * title of a movie to watch
     */
    private void promptUser( String dir, String filename, Principal p ){
	String path = searchMovie(dir, filename);
	System.out.println("*** Your subscription : "+p.getName()+" ***");
	if( path.length() == 0 || !(new File(path)).canRead() )
	    System.out.println(filename + " is not in your subscription! ");
	else{
	    try{
		BufferedReader br = new BufferedReader( new FileReader(path) );
		String data = br.readLine();
		System.out.println("\n ------------------------------");
		System.out.println("\t"+filename);
		System.out.println(data);
		System.out.println("\n ------------------------------");
	    }catch( Exception e ){
		System.out.println( e.toString() );
		System.exit(1);
	    }
	}
    }

    /**
     * <p> This Sample PrivilegedAction performs the following operations:
     * <ul>
     * <li> Access the System property, <i>java.home</i>
     * <li> Access the System property, <i>user.home</i>
     * <li> Access the file, <i>foo.txt</i>
     * </ul>
     *
     * @return <code>null</code> in all cases.
     *
     * @exception SecurityException if the caller does not have permission
     *          to perform the operations listed above.
     */
    public Object run() {
	Subject currentSubject = Subject.getSubject(AccessController.getContext());
	Iterator iter = currentSubject.getPrincipals().iterator();
	String dir = "";
	Principal p = null;
	while( iter.hasNext() ){
	    p = (Principal)iter.next();
	    dir = dir + p.getName() + " ";
	}
	if( (p.getName()).equals("Admin") ){
	    String choice;
	    while( !(choice = System.console().readLine("choice : ")).equals("exit") ){
		if( choice.equals("add") ){
		    String directory = System.console().readLine("directory : ");
		    String filename = System.console().readLine("name : ");
		    String content = System.console().readLine("content : ");
		    try{
			BufferedWriter bw = new BufferedWriter( new FileWriter("sample/db/"+directory+"/"+filename+".txt") );
			bw.write(content);
			bw.close();
			System.out.println(filename+".txt generated.");
		    }catch( Exception e ){
			System.out.println( e.toString() );
			continue;
		    }
		}
		else if( choice.equals("rm") ){
		    try{
			String filename = System.console().readLine("name : ");
			File f = new File(searchMovie(dir, filename));
			if( f.exists() ){
			    f.delete();
			    System.out.println(filename+".txt deleted.");
			}
		    }catch( Exception e ){
			System.out.println(e.toString());
			continue;
		    }
		}
		else if( choice.equals("read") ){
		    String filename = System.console().readLine("Movie to watch : ");
		    promptUser( dir, filename, p );
		}
		else{
		    System.out.println("Invalid selection ( add, rm, read, exit )");
		}
	    }
	}
	else{
	    String filename;
	    while(!(filename = System.console().readLine("Movie to watch : ")).equals("exit")){
		promptUser( dir, filename, p );
	    }
	}
        return null;
    }
}
