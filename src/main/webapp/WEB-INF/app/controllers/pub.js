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

var pub = {
    getLastModified: function(req) {
	    return fileLastModified(this.getPublicPath(req));
	},

	get: function(req, res) {
		try {
			readFile(this.getPublicPath(req), res.getOutputStream());
		} catch (e) {
			// file not found
			res.setStatus(404);
		}
    },
    
    getPublicPath: function(req) {
        var publicPath = req.getAttribute("com.joelhockey.cirrus.public_path");
        if (publicPath) { return publicPath; }
        var dirs = pathdirs;
        // default file for '/' is 'index.html'
        if (!dirs[0]) {
        	dirs = ["index.html"];
        }
        // add '.html' suffix if no file type given
        if (dirs[dirs.length - 1].indexOf(".") == -1) {
        	dirs[dirs.length -1] += ".html";
        }
        var publicPath = "/WEB-INF/public/" + dirs.join("/");
        req.setAttribute("com.joelhockey.cirrus.public_path", publicPath);
        return publicPath;
    }
}
