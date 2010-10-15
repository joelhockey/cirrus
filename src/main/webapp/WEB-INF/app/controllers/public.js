/**
 * The MIT Licence
 *
 * Copyright 2010 Joel Hockey (joel.hockey@gmail.com).  All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

CONTROLLERS["public"] = {
    publicPath: null,
    before: function () {
	    // default file for '/' is 'index.html'
	    if (pathdirs.length === 0) {
	    	pathdirs = ["index"];
	    }
	    // add '.html' suffix if no file type given
	    if (pathdirs[pathdirs.length - 1].lastIndexOf(".") == -1) {
	        pathdirs[pathdirs.length - 1] += ".html";
	    }
	    this.publicPath = "/WEB-INF/public/" + pathdirs.join("/");
    },

    getLastModified: function () {
	    return fileLastModified(this.publicPath);
	},
	
	GET: {
	    index: function() {
	        res.sendRedirect("/login");
	    },
		$: function () {
			try {
				// set Content-Type
				var fileSuffix = this.publicPath.substring(this.publicPath.lastIndexOf(".") + 1);
				var contentType = MIME_TYPES[fileSuffix];
				if (contentType) {
					res.setContentType(contentType);
				}
				log("using Content-Type: " + contentType + ", for file: " + this.publicPath);
				readFile(this.publicPath, res.getOutputStream());
			} catch (e) {
				logwarn("error sending static file: " + this.publicPath, e);
				throw 404;
			}
		}
    }
};
