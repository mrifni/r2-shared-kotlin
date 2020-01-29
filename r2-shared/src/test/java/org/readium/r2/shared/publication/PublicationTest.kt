/*
 * Module: r2-shared-kotlin
 * Developers: Mickaël Menu
 *
 * Copyright (c) 2020. Readium Foundation. All rights reserved.
 * Use of this source code is governed by a BSD-style license which is detailed in the
 * LICENSE file present in the project repository where this source code is maintained.
 */

package org.readium.r2.shared.publication

import org.junit.Assert.*
import org.junit.Test
import org.readium.r2.shared.publication.webpub.LocalizedString
import org.readium.r2.shared.publication.webpub.ReadingProgression
import org.readium.r2.shared.publication.webpub.WebPublication
import org.readium.r2.shared.publication.webpub.link.Link
import org.readium.r2.shared.publication.webpub.metadata.Metadata

class PublicationTest {

    private fun createPublication(
        title: String = "Title",
        language: String = "EN",
        readingProgression: ReadingProgression = ReadingProgression.AUTO,
        links: List<Link> = listOf(),
        readingOrder: List<Link> = emptyList(),
        resources: List<Link> = emptyList()
    ) = Publication(
        webpub = WebPublication(
            metadata = Metadata(
                localizedTitle = LocalizedString(title),
                languages = listOf(language),
                readingProgression = readingProgression
            ),
            links = links,
            readingOrder = readingOrder,
            resources = resources
        )
    )

    @Test fun `get {contentLayout} for the default language`() {
        assertEquals(
            ContentLayout.RTL,
            createPublication(language = "AR").contentLayout
        )
    }

    @Test fun `get {contentLayout} for the given language`() {
        val publication = createPublication()

        assertEquals(ContentLayout.RTL, publication.contentLayoutForLanguage("AR"))
        assertEquals(ContentLayout.LTR, publication.contentLayoutForLanguage("EN"))
    }

    @Test fun `get {contentLayout} fallbacks on the {readingProgression}`() {
        assertEquals(
            ContentLayout.RTL,
            createPublication(
                language = "en",
                readingProgression = ReadingProgression.RTL
            ).contentLayoutForLanguage("EN")
        )
    }

    @Test fun `find the first {Link} matching the given predicate`() {
        val link1 = Link(href = "href1", title = "link1")
        val link2 = Link(href = "href2", title = "link2")
        val link3 = Link(href = "href3", title = "link3")

        val publication = createPublication(
            links = listOf(Link(href = "other"), link1),
            readingOrder = listOf(Link(href = "other"), link2),
            resources = listOf(Link(href = "other"), link3)
        )

        fun predicate(title: String) =
            { link: Link -> link.title == title}

        assertEquals(link1, publication.link(predicate("link1")))
        assertEquals(link2, publication.link(predicate("link2")))
        assertEquals(link3, publication.link(predicate("link3")))
    }

    @Test fun `find the first {Link} with the given predicate when not found`() {
        assertNull(createPublication().link { it.href == "foobar" })
    }

    @Test fun `find the first {Link} with the given {rel}`() {
        val link1 = Link(href = "found", rels = listOf("rel1"))
        val link2 = Link(href = "found", rels = listOf("rel2"))
        val link3 = Link(href = "found", rels = listOf("rel3"))
        val publication = createPublication(
            links = listOf(Link(href = "other"), link1),
            readingOrder = listOf(Link(href = "other"), link2),
            resources = listOf(Link(href = "other"), link3)
        )

        assertEquals(link1, publication.linkWithRel("rel1"))
        assertEquals(link2, publication.linkWithRel("rel2"))
        assertEquals(link3, publication.linkWithRel("rel3"))
    }

    @Test fun `find the first {Link} with the given {rel} when missing`() {
        assertNull(createPublication().linkWithRel("foobar"))
    }

    @Test fun `find the first {Link} with the given {href}`() {
        val link1 = Link(href = "href1")
        val link2 = Link(href = "href2")
        val link3 = Link(href = "href3")
        val publication = createPublication(
            links = listOf(Link(href = "other"), link1),
            readingOrder = listOf(Link(href = "other"), link2),
            resources = listOf(Link(href = "other"), link3)
        )

        assertEquals(link1, publication.linkWithHref("href1"))
        assertEquals(link2, publication.linkWithHref("href2"))
        assertEquals(link3, publication.linkWithHref("href3"))
    }

    @Test fun `find the first {Link} with the given {href} when missing`() {
        assertNull(createPublication().linkWithHref("foobar"))
    }

    @Test fun `find the first resource {Link} with the given {href}`() {
        val link1 = Link(href = "href1")
        val link2 = Link(href = "href2")
        val link3 = Link(href = "href3")
        val publication = createPublication(
            links = listOf(Link(href = "other"), link1),
            readingOrder = listOf(Link(href = "other"), link2),
            resources = listOf(Link(href = "other"), link3)
        )

        assertNull(publication.resourceWithHref("href1"))
        assertEquals(link2, publication.resourceWithHref("href2"))
        assertEquals(link3, publication.resourceWithHref("href3"))
    }

    @Test fun `find the first resource {Link} with the given {href} when missing`() {
        assertNull(createPublication().resourceWithHref("foobar"))
    }

    @Test fun `find the cover {Link}`() {
        val coverLink = Link(href = "cover", rels = listOf("cover"))
        val publication = createPublication(
            links = listOf(Link(href = "other"), coverLink),
            readingOrder = listOf(Link(href = "other")),
            resources = listOf(Link(href = "other"))
        )

        assertEquals(coverLink, publication.coverLink)
    }

    @Test fun `find the cover {Link} when missing`() {
        val publication = createPublication(
            links = listOf(Link(href = "other")),
            readingOrder = listOf(Link(href = "other")),
            resources = listOf(Link(href = "other"))
        )

        assertNull(publication.coverLink)
    }

    @Test fun `set {self} link`() {
        val publication = createPublication()
        publication.setSelfLink("http://manifest.json")

        assertEquals(
            "http://manifest.json",
            publication.linkWithRel("self")?.href
        )
    }

    @Test fun `set {self} link replaces existing {self} link`() {
        val publication = createPublication(
            links = listOf(Link(href = "previous", rels = listOf("self")))
        )
        publication.setSelfLink("http://manifest.json")

        assertEquals(
            "http://manifest.json",
            publication.linkWithRel("self")?.href
        )
    }


    // Publication.Format

    @Test fun `parse format from single mimetype`() {
        assertEquals(Publication.Format.EPUB, Publication.Format.from("application/epub+zip"))
        assertEquals(Publication.Format.UNKNOWN, Publication.Format.from("foobar"))
        assertEquals(Publication.Format.UNKNOWN, Publication.Format.from(null))
    }

    @Test fun `parse format from single mimetype fallbacks on {fileExtension}`() {
        assertEquals(Publication.Format.EPUB, Publication.Format.from("application/epub+zip", fileExtension = "pdf"))
        assertEquals(Publication.Format.PDF, Publication.Format.from("foobar", fileExtension = "pdf"))
    }

    @Test fun `parse format from mimetypes`() {
        assertEquals(Publication.Format.EPUB, Publication.Format.from(listOf("foobar", "application/epub+zip")))
        assertEquals(Publication.Format.UNKNOWN, Publication.Format.from(listOf("foobar")))
    }

    @Test fun `parse format from mimetypes fallbacks on {fileExtension}`() {
        assertEquals(Publication.Format.PDF, Publication.Format.from(listOf("foobar"), fileExtension = "pdf"))
    }

    @Test fun `parse format from EPUB mimetypes`() {
        assertEquals(Publication.Format.EPUB, Publication.Format.from("application/epub+zip"))
        assertEquals(Publication.Format.EPUB, Publication.Format.from("application/oebps-package+xml"))
    }

    @Test fun `parse format from CBZ mimetypes`() {
        assertEquals(Publication.Format.CBZ, Publication.Format.from("application/x-cbr"))
    }

    @Test fun `parse format from PDF mimetypes`() {
        assertEquals(Publication.Format.PDF, Publication.Format.from("application/pdf"))
        assertEquals(Publication.Format.PDF, Publication.Format.from("application/pdf+lcp"))
    }

    @Test fun `parse format from WEBPUB mimetypes`() {
        assertEquals(Publication.Format.WEBPUB, Publication.Format.from("application/webpub+json"))
    }

    @Test fun `parse format from AUDIOBOOK mimetypes`() {
        assertEquals(Publication.Format.AUDIOBOOK, Publication.Format.from("application/audiobook+zip"))
        assertEquals(Publication.Format.AUDIOBOOK, Publication.Format.from("application/audiobook+json"))
    }

    @Test fun `parse format from EPUB extensions`() {
        assertEquals(Publication.Format.EPUB, Publication.Format.from(null, fileExtension = "epub"))
        assertEquals(Publication.Format.EPUB, Publication.Format.from(null, fileExtension = "EPUB"))
    }

    @Test fun `parse format from CBZ extensions`() {
        assertEquals(Publication.Format.CBZ, Publication.Format.from(null, fileExtension = "cbz"))
        assertEquals(Publication.Format.CBZ, Publication.Format.from(null, fileExtension = "CBZ"))
    }

    @Test fun `parse format from PDF extensions`() {
        assertEquals(Publication.Format.PDF, Publication.Format.from(null, fileExtension = "pdf"))
        assertEquals(Publication.Format.PDF, Publication.Format.from(null, fileExtension = "PDF"))
        assertEquals(Publication.Format.PDF, Publication.Format.from(null, fileExtension = "lcpdf"))
        assertEquals(Publication.Format.PDF, Publication.Format.from(null, fileExtension = "LCPDF"))
    }

    @Test fun `parse format from WEBPUB extensions`() {
        assertEquals(Publication.Format.WEBPUB, Publication.Format.from(null, fileExtension = "json"))
        assertEquals(Publication.Format.WEBPUB, Publication.Format.from(null, fileExtension = "JSON"))
    }

    @Test fun `parse format from AUDIOBOOK extensions`() {
        assertEquals(Publication.Format.AUDIOBOOK, Publication.Format.from(null, fileExtension = "audiobook"))
    }

}