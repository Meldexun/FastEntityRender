package meldexun.fastentityrender.opengl;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.ARBBufferStorage;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GLContext;

public enum BufferStorage {

	OpenGL45 {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return capabilities.OpenGL45;
		}

		@Override
		protected int _createBuffer() {
			return GL45.glCreateBuffers();
		}

		@Override
		protected void _deleteBuffer(int buffer) {
			GL15.glDeleteBuffers(buffer);
		}

		@Override
		protected void _bindBuffer(int target, int buffer, boolean required) {
			if (required) {
				GL15.glBindBuffer(target, buffer);
			}
		}

		@Override
		protected void _initBuffer(int target, int buffer, long size, int flags) {
			GL45.glNamedBufferStorage(buffer, size, flags);
		}

		@Override
		protected ByteBuffer _mapBuffer(int target, int buffer, long offset, long length, int access) {
			return GL45.glMapNamedBufferRange(buffer, offset, length, access, null);
		}

		@Override
		protected void _flushBuffer(int target, int buffer, long offset, long length) {
			GL45.glFlushMappedNamedBufferRange(buffer, offset, length);
		}

		@Override
		protected void _unmapBuffer(int target, int buffer) {
			GL45.glUnmapNamedBuffer(buffer);
		}
	},
	ARBDirectStateAccess_ {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return capabilities.GL_ARB_direct_state_access;
		}

		@Override
		protected int _createBuffer() {
			return ARBDirectStateAccess.glCreateBuffers();
		}

		@Override
		protected void _deleteBuffer(int buffer) {
			GL15.glDeleteBuffers(buffer);
		}

		@Override
		protected void _bindBuffer(int target, int buffer, boolean required) {
			if (required) {
				GL15.glBindBuffer(target, buffer);
			}
		}

		@Override
		protected void _initBuffer(int target, int buffer, long size, int flags) {
			ARBDirectStateAccess.glNamedBufferStorage(buffer, size, flags);
		}

		@Override
		protected ByteBuffer _mapBuffer(int target, int buffer, long offset, long length, int access) {
			return ARBDirectStateAccess.glMapNamedBufferRange(buffer, offset, length, access, null);
		}

		@Override
		protected void _flushBuffer(int target, int buffer, long offset, long length) {
			ARBDirectStateAccess.glFlushMappedNamedBufferRange(buffer, offset, length);
		}

		@Override
		protected void _unmapBuffer(int target, int buffer) {
			ARBDirectStateAccess.glUnmapNamedBuffer(buffer);
		}
	},
	OpenGL44 {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return capabilities.OpenGL44;
		}

		@Override
		protected int _createBuffer() {
			return GL15.glGenBuffers();
		}

		@Override
		protected void _deleteBuffer(int buffer) {
			GL15.glDeleteBuffers(buffer);
		}

		@Override
		protected void _bindBuffer(int target, int buffer, boolean required) {
			GL15.glBindBuffer(target, buffer);
		}

		@Override
		protected void _initBuffer(int target, int buffer, long size, int flags) {
			GL44.glBufferStorage(target, size, flags);
		}

		@Override
		protected ByteBuffer _mapBuffer(int target, int buffer, long offset, long length, int access) {
			return GL30.glMapBufferRange(target, offset, length, access, null);
		}

		@Override
		protected void _flushBuffer(int target, int buffer, long offset, long length) {
			GL30.glFlushMappedBufferRange(target, offset, length);
		}

		@Override
		protected void _unmapBuffer(int target, int buffer) {
			GL15.glUnmapBuffer(target);
		}
	},
	ARBBufferStorage_ {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return capabilities.GL_ARB_buffer_storage;
		}

		@Override
		protected int _createBuffer() {
			return GL15.glGenBuffers();
		}

		@Override
		protected void _deleteBuffer(int buffer) {
			GL15.glDeleteBuffers(buffer);
		}

		@Override
		protected void _bindBuffer(int target, int buffer, boolean required) {
			GL15.glBindBuffer(target, buffer);
		}

		@Override
		protected void _initBuffer(int target, int buffer, long size, int flags) {
			ARBBufferStorage.glBufferStorage(target, size, flags);
		}

		@Override
		protected ByteBuffer _mapBuffer(int target, int buffer, long offset, long length, int access) {
			return GL30.glMapBufferRange(target, offset, length, access, null);
		}

		@Override
		protected void _flushBuffer(int target, int buffer, long offset, long length) {
			GL30.glFlushMappedBufferRange(target, offset, length);
		}

		@Override
		protected void _unmapBuffer(int target, int buffer) {
			GL15.glUnmapBuffer(target);
		}
	},
	UNSUPPORTED {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return true;
		}

		@Override
		protected int _createBuffer() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void _deleteBuffer(int buffer) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void _bindBuffer(int target, int buffer, boolean required) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void _initBuffer(int target, int buffer, long size, int flags) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected ByteBuffer _mapBuffer(int target, int buffer, long offset, long length, int access) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void _flushBuffer(int target, int buffer, long offset, long length) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void _unmapBuffer(int target, int buffer) {
			throw new UnsupportedOperationException();
		}
	};

	private static BufferStorage supported;

	public static BufferStorage get() {
		BufferStorage supported;
		if ((supported = BufferStorage.supported) == null) {
			ContextCapabilities capabilities = GLContext.getCapabilities();
			for (BufferStorage bufferStorage : BufferStorage.values()) {
				if (bufferStorage._isSupported(capabilities)) {
					BufferStorage.supported = supported = bufferStorage;
					break;
				}
			}
		}
		return supported;
	}

	protected abstract boolean _isSupported(ContextCapabilities capabilities);

	protected abstract int _createBuffer();

	protected abstract void _deleteBuffer(int buffer);

	protected abstract void _bindBuffer(int target, int buffer, boolean optional);

	protected abstract void _initBuffer(int target, int buffer, long size, int flags);

	protected abstract ByteBuffer _mapBuffer(int target, int buffer, long offset, long length, int access);

	protected abstract void _flushBuffer(int target, int buffer, long offset, long length);

	protected abstract void _unmapBuffer(int target, int buffer);

	public static boolean isSupported() {
		return get() != UNSUPPORTED;
	}

	public static int createBuffer() {
		return get()._createBuffer();
	}

	public static void deleteBuffer(int buffer) {
		get()._deleteBuffer(buffer);
	}

	public static void bindBuffer(int target, int buffer, boolean required) {
		get()._bindBuffer(target, buffer, required);
	}

	public static void initBuffer(int target, int buffer, long size, int flags) {
		get()._initBuffer(target, buffer, size, flags);
	}

	public static ByteBuffer mapBuffer(int target, int buffer, long offset, long length, int access) {
		return get()._mapBuffer(target, buffer, offset, length, access);
	}

	public static void flushBuffer(int target, int buffer, long offset, long length) {
		get()._flushBuffer(target, buffer, offset, length);
	}

	public static void unmapBuffer(int target, int buffer) {
		get()._unmapBuffer(target, buffer);
	}

}
