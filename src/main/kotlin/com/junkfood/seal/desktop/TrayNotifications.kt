package com.junkfood.seal.desktop

import java.awt.Color
import java.awt.Font
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

internal object TrayNotifications {
    private var trayIcon: TrayIcon? = null

    @Synchronized
    fun show(title: String, message: String, failed: Boolean): Boolean {
        if (!SystemTray.isSupported()) return false
        val tray = SystemTray.getSystemTray()
        val icon =
            trayIcon ?: TrayIcon(createImage(), "Seal Desktop").apply {
                isImageAutoSize = true
                tray.add(this)
                trayIcon = this
            }
        icon.displayMessage(
            title,
            message.take(240),
            if (failed) TrayIcon.MessageType.ERROR else TrayIcon.MessageType.INFO,
        )
        return true
    }

    @Synchronized
    fun remove() {
        trayIcon?.let(SystemTray.getSystemTray()::remove)
        trayIcon = null
    }

    private fun createImage(): BufferedImage =
        BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB).apply {
            val graphics = createGraphics()
            try {
                graphics.color = Color(0xE5, 0x39, 0x35)
                graphics.fillRoundRect(0, 0, 32, 32, 9, 9)
                graphics.color = Color.WHITE
                graphics.font = Font(Font.SANS_SERIF, Font.BOLD, 23)
                graphics.drawString("S", 7, 24)
            } finally {
                graphics.dispose()
            }
        }
}
