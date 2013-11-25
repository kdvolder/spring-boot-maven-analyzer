package org.springsource.ide.eclipse.boot.maven.analyzer.util;

import java.io.InputStream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class AsmUtils {
	
	private static class IsPublicVisitor implements ClassVisitor {
		
		public boolean isPublic = false;

		@Override
		public void visit(int version, int access, String name, String sig, String superName, String[] interfaces) {
			isPublic = (Opcodes.ACC_PUBLIC&access)!=0;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void visitAttribute(Attribute arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visitEnd() {
			// TODO Auto-generated method stub

		}

		@Override
		public FieldVisitor visitField(int arg0, String arg1, String arg2,
				String arg3, Object arg4) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void visitInnerClass(String arg0, String arg1, String arg2,
				int arg3) {
			// TODO Auto-generated method stub

		}

		@Override
		public MethodVisitor visitMethod(int arg0, String arg1, String arg2,
				String arg3, String[] arg4) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void visitOuterClass(String arg0, String arg1, String arg2) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visitSource(String arg0, String arg1) {
			// TODO Auto-generated method stub

		}

	}

	/**
	 * Uses ASM to read a .class file and determine if it contains a public type.
	 */
	public static boolean isPublic(InputStream classFileInput) {
		try {
			ClassReader reader = new ClassReader(classFileInput);
			IsPublicVisitor isPublicVisitor = new IsPublicVisitor();
			reader.accept(isPublicVisitor, ClassReader.SKIP_CODE);
			return isPublicVisitor.isPublic;
		} catch (Exception e) {
			Logger.log(e);
		}
		return false;
	}
}
