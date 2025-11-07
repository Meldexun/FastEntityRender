package meldexun.fastentityrender.opengl;

import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.ARBVertexArrayObject;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GLContext;

public enum VertexArray {

	OpenGL45 {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return capabilities.OpenGL45;
		}

		@Override
		protected int _createVertexArray() {
			return GL45.glCreateVertexArrays();
		}

		@Override
		protected void _deleteVertexArray(int vertexArray) {
			GL30.glDeleteVertexArrays(vertexArray);
		}

		@Override
		protected void _bindVertexArray(int vertexArray) {
			GL30.glBindVertexArray(vertexArray);
		}
	},
	_ARBDirectStateAccess {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return capabilities.GL_ARB_direct_state_access;
		}

		@Override
		protected int _createVertexArray() {
			return ARBDirectStateAccess.glCreateVertexArrays();
		}

		@Override
		protected void _deleteVertexArray(int vertexArray) {
			GL30.glDeleteVertexArrays(vertexArray);
		}

		@Override
		protected void _bindVertexArray(int vertexArray) {
			GL30.glBindVertexArray(vertexArray);
		}
	},
	OpenGL30 {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return capabilities.OpenGL30;
		}

		@Override
		protected int _createVertexArray() {
			return GL30.glGenVertexArrays();
		}

		@Override
		protected void _deleteVertexArray(int vertexArray) {
			GL30.glDeleteVertexArrays(vertexArray);
		}

		@Override
		protected void _bindVertexArray(int vertexArray) {
			GL30.glBindVertexArray(vertexArray);
		}
	},
	_ARBVertexArrayObject {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return capabilities.GL_ARB_vertex_array_object;
		}

		@Override
		protected int _createVertexArray() {
			return ARBVertexArrayObject.glGenVertexArrays();
		}

		@Override
		protected void _deleteVertexArray(int vertexArray) {
			ARBVertexArrayObject.glDeleteVertexArrays(vertexArray);
		}

		@Override
		protected void _bindVertexArray(int vertexArray) {
			ARBVertexArrayObject.glBindVertexArray(vertexArray);
		}
	},
	UNSUPPORTED {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return true;
		}

		@Override
		protected int _createVertexArray() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void _deleteVertexArray(int vertexArray) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void _bindVertexArray(int vertexArray) {
			throw new UnsupportedOperationException();
		}
	};

	private static VertexArray supported;

	private static VertexArray get() {
		VertexArray supported;
		if ((supported = VertexArray.supported) == null) {
			ContextCapabilities capabilities = GLContext.getCapabilities();
			for (VertexArray vertexArray : VertexArray.values()) {
				if (vertexArray._isSupported(capabilities)) {
					supported = VertexArray.supported = vertexArray;
					break;
				}
			}
		}
		return supported;
	}

	protected abstract boolean _isSupported(ContextCapabilities capabilities);

	protected abstract int _createVertexArray();

	protected abstract void _deleteVertexArray(int vertexArray);

	protected abstract void _bindVertexArray(int vertexArray);

	public static boolean isSupported() {
		return get() != UNSUPPORTED;
	}

	public static int createVertexArray() {
		return get()._createVertexArray();
	}

	public static void deleteVertexArray(int vertexArray) {
		get()._deleteVertexArray(vertexArray);
	}

	public static void bindVertexArray(int vertexArray) {
		get()._bindVertexArray(vertexArray);
	}

}
