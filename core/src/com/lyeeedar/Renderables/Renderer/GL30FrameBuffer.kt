package com.badlogic.gdx.graphics.glutils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter
import com.badlogic.gdx.graphics.Texture.TextureWrap

class GL30FrameBuffer(internalFormat: Int, format: Int, type: Int, width: Int, height: Int, hasDepth: Boolean, hasStencil: Boolean = false) : GLFrameBuffer<Texture?>()
{
	init
	{
		val frameBufferBuilder = FrameBufferBuilder(width, height)
		frameBufferBuilder.addColorTextureAttachment(internalFormat, format, type)
		if (hasDepth) frameBufferBuilder.addBasicDepthRenderBuffer()
		if (hasStencil) frameBufferBuilder.addBasicStencilRenderBuffer()
		bufferBuilder = frameBufferBuilder
		build()
	}

	override fun createTexture(attachmentSpec: FrameBufferTextureAttachmentSpec): Texture
	{
		val data = GLOnlyTextureData(bufferBuilder.width, bufferBuilder.height, 0, attachmentSpec.internalFormat, attachmentSpec.format, attachmentSpec.type)
		val result = Texture(data)
		result.setFilter(TextureFilter.Linear, TextureFilter.Linear)
		result.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge)
		return result
	}

	protected override fun disposeColorTexture(colorTexture: Texture?)
	{
		colorTexture!!.dispose()
	}

	protected override fun attachFrameBufferColorTexture(texture: Texture?)
	{
		Gdx.gl20.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, GL20.GL_TEXTURE_2D, texture!!.textureObjectHandle, 0)
	}

	companion object
	{
		fun unbind()
		{
			GLFrameBuffer.unbind()
		}
	}
}