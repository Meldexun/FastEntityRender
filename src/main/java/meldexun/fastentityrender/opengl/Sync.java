package meldexun.fastentityrender.opengl;

import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.ARBSync;
import org.lwjgl.opengl.ARBTimerQuery;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.opengl.GLSync;

public enum Sync {

	OpenGL45 {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return capabilities.OpenGL45;
		}

		@Override
		protected Object _createSync() {
			return GL45.glCreateQueries(GL33.GL_TIMESTAMP);
		}

		@Override
		protected void _deleteSync(Object sync) {
			GL15.glDeleteQueries((int) sync);
		}

		@Override
		protected void _waitSync(Object sync) {
			GL33.glGetQueryObjectui64((int) sync, GL15.GL_QUERY_RESULT);
		}
	},
	_ARBDirectStateAccess {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return capabilities.GL_ARB_direct_state_access;
		}

		@Override
		protected Object _createSync() {
			return ARBDirectStateAccess.glCreateQueries(GL33.GL_TIMESTAMP);
		}

		@Override
		protected void _deleteSync(Object sync) {
			GL15.glDeleteQueries((int) sync);
		}

		@Override
		protected void _waitSync(Object sync) {
			GL33.glGetQueryObjectui64((int) sync, GL15.GL_QUERY_RESULT);
		}
	},
	OpenGL33 {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return capabilities.OpenGL33;
		}

		@Override
		protected Object _createSync() {
			int sync = GL15.glGenQueries();
			GL33.glQueryCounter(sync, GL33.GL_TIMESTAMP);
			return sync;
		}

		@Override
		protected void _deleteSync(Object sync) {
			GL15.glDeleteQueries((int) sync);
		}

		@Override
		protected void _waitSync(Object sync) {
			GL33.glGetQueryObjectui64((int) sync, GL15.GL_QUERY_RESULT);
		}
	},
	_ARBTimerQuery {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return capabilities.GL_ARB_timer_query;
		}

		@Override
		protected Object _createSync() {
			int sync = GL15.glGenQueries();
			ARBTimerQuery.glQueryCounter(sync, ARBTimerQuery.GL_TIMESTAMP);
			return sync;
		}

		@Override
		protected void _deleteSync(Object sync) {
			GL15.glDeleteQueries((int) sync);
		}

		@Override
		protected void _waitSync(Object sync) {
			ARBTimerQuery.glGetQueryObjectui64((int) sync, GL15.GL_QUERY_RESULT);
		}
	},
	OpenGL32 {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return capabilities.OpenGL32;
		}

		@Override
		protected Object _createSync() {
			return GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
		}

		@Override
		protected void _deleteSync(Object sync) {
			GL32.glDeleteSync((GLSync) sync);
		}

		@Override
		protected void _waitSync(Object sync) {
			GL32.glClientWaitSync((GLSync) sync, 0, Long.MAX_VALUE);
		}
	},
	_ARBSync {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return capabilities.GL_ARB_sync;
		}

		@Override
		protected Object _createSync() {
			return ARBSync.glFenceSync(ARBSync.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
		}

		@Override
		protected void _deleteSync(Object sync) {
			ARBSync.glDeleteSync((GLSync) sync);
		}

		@Override
		protected void _waitSync(Object sync) {
			ARBSync.glClientWaitSync((GLSync) sync, 0, Long.MAX_VALUE);
		}
	},
	UNSUPPORTED {
		@Override
		protected boolean _isSupported(ContextCapabilities capabilities) {
			return true;
		}

		@Override
		protected Object _createSync() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void _deleteSync(Object sync) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void _waitSync(Object sync) {
			throw new UnsupportedOperationException();
		}
	};

	private static Sync supported;

	private static Sync get() {
		Sync supported;
		if ((supported = Sync.supported) == null) {
			ContextCapabilities capabilities = GLContext.getCapabilities();
			for (Sync sync : Sync.values()) {
				if (sync._isSupported(capabilities)) {
					supported = Sync.supported = sync;
					break;
				}
			}
		}
		return supported;
	}

	protected abstract boolean _isSupported(ContextCapabilities capabilities);

	protected abstract Object _createSync();

	protected abstract void _deleteSync(Object sync);

	protected abstract void _waitSync(Object sync);

	public static boolean isSupported() {
		return get() != UNSUPPORTED;
	}

	public static Object createSync() {
		return get()._createSync();
	}

	public static void deleteSync(Object sync) {
		get()._deleteSync(sync);
	}

	public static void waitSync(Object sync) {
		get()._waitSync(sync);
	}

}
