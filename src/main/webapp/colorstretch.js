/*
 * colorstretch - utilities to stretch colormap of LSST images
 */

var ColorStretch = {};

(function($) {

//=================================================================
// decoders:  functions which map rgba to scalar
//=================================================================
  $.decoders = {};

  //---------------------------------------------------------------
  // raw 18-bit pixel values encoded in RGB fields
  //---------------------------------------------------------------
  $.decoders.raw = function(r, g, b, a) {
    return (r << 10) | (g << 2) | (b >> 6) | 0;
  }

//=================================================================
// colormaps:  functions which map scalar 0..1 to rgba.
// Giving these functions a value < 0 returns the underflow array.
// A value >= 1 returns the overflow array.
//=================================================================
  $.colormaps = {};

  //---------------------------------------------------------------
  // set these as values to return for underflow and overflow
  //---------------------------------------------------------------
  $.colormaps.underflow = [ 0, 0, 0, 0 ];
  $.colormaps.overflow = [ 255, 255, 255, 255 ];

  //---------------------------------------------------------------
  // utility function to linearly interpolate between fixed points
  //   x = input argument
  //   points = array of fixed points in x.  points[0] should be 0.
  //   values = array of values at the corresponding fixed points.
  // return value:  scalar function value
  //---------------------------------------------------------------
  $.colormaps.interpolate = function(x, points, values) {
    for (let i = 0; i < points.length; i++) {
      if (x < points[i]) {
        return (values[i-1] + (values[i] - values[i-1]) *
                (x - points[i-1]) / (points[i] - points[i-1])) | 0;
      }
    }
    // overflow
    return (values[values.length - 1]) | 0;
  }

  //---------------------------------------------------------------
  // specific colormaps
  //---------------------------------------------------------------

  $.colormaps.gray = function(x) {
    if (x < 0.0) return underflow;
    if (x >= 1.0) return overflow;
    const v = (256 * x) | 0;
    return [v, v, v, 255];
  };

  $.colormaps.saoA = function(x) {
    if (x < 0.0) return underflow;
    else if (x < 1.0)
      return [ $.colormaps.interpolate(x, [0, 0.25, 0.5,   1],
                                          [0,    0, 255, 255]),
               $.colormaps.interpolate(x, [0, 0.25, 0.5, 0.75,   1],
                                          [0,  255,   0,    0, 255]),
               $.colormaps.interpolate(x, [0, 0.125, 0.5, 0.75, 1],
                                          [0,     0, 255,    0, 0]),
               255 ];
    else return overflow;
  }

  //===============================================================
  // colormap histogram
  //   1. histogram counts the number of pixels with a certain
  //      pixel value.
  //   2. one can then generate a stretcher function which maps
  //      a pixel value to rgba.
  //   3. the histogram and colorbar can also be drawn.
  //
  // The horizontal axis of the histogram is the pixel value,
  // and the vertical axis the number of pixels with that value.
  //===============================================================

  //---------------------------------------------------------------
  // constructor
  //   bins = 1D array of binned data (assumes equal-spaced bins)
  //   xlo = left edge of binned data
  //   xhi = right edge of binned data
  //---------------------------------------------------------------
  $.Histogram = function(bins, xlo, xhi) {
    this.data = bins;
    this.xlo = xlo;
    this.xhi = xhi;
    //console.log('new Histogram: xlo=' + xlo + ' xhi=' + xhi +
    //            ' nbins=' + bins.length);
  }

  $.Histogram.prototype = {

    //-------------------------------------------------------------
    // queries
    //-------------------------------------------------------------

    // number of bins in histogram object
    nbins: function() { return this.data.length; },
 
    // maximum bin contents
    // (assumes bin contents are non-zero)
    yhi: function() {
      let ym = -1;
      for (let i = 0; i < this.data.length; i++) {
        if (this.data[i] > ym) ym = this.data[i];
      }
      return ym;
    },

    // bin width
    dx: function() { return (this.xhi - this.xlo) / this.data.length; },

    //-------------------------------------------------------------
    // fill histogram
    //   x = (number or array of numbers) values to fill
    // return value:  the histogram object
    //-------------------------------------------------------------
    fill: function(x) {
      if (x.isArray()) {
        for (let j = 0; j < x.length; j++) {
          if (x[j] >= this.xlo && x[j] < this.xhi) {
            const i = ((x[j]-this.xlo)*this.data.length/(this.xhi-this.xlo))|0;
            this.data[i] += 1;
          }
        }
      } else {
        if (x >= this.xlo && x < this.xhi) {
          const i = ((x-this.xlo)*this.data.length / (this.xhi-this.xlo))|0;
          this.data[i] += 1;
        }
      }
      return this;
    },

    //-------------------------------------------------------------
    // truncate a value to a specified granularity
    //   w = value to truncate
    //   ndiv = target number of divisions
    // return value:  the truncated value
    //-------------------------------------------------------------
    truncate: function(w, ndiv) {
      let n = w / ndiv; // initial bin width
      let tens = 1;
      while (n > 10) {
        n /= 10;
        tens *= 10;
      }
      n = (n | 0);
      if (n > 5) n = 5;
      else if (n > 2) n = 2;
      return n*tens;
    },

    //-------------------------------------------------------------
    // figure out how many tics there should be
    // based on how many digits in x width.
    // return:  array of x values for the tics
    //-------------------------------------------------------------
    getXTics: function() {
      const binwidth = this.truncate(this.xhi - this.xlo, 5);
      let v = [];
      for (let x = Math.ceil(this.xlo / binwidth) * binwidth;
           x < this.xhi; x += binwidth) {
        v.push(x);
      }
      return v;
    },

    //-------------------------------------------------------------
    // figure out how many tics there should be in y.
    // assume the bottom of the histogram is 0.
    // return:  array of y values for the tics
    //-------------------------------------------------------------
    getYTics: function() {
      const binwidth = this.truncate(this.yhi(), 5);
      let v = [];
      let yh = this.yhi();
      for (let y = 0; y < yh; y += binwidth) v.push(y);
      return v;
    },

    //-------------------------------------------------------------
    // figure out how many tics there should be in y,
    // with logarithmic spacing.
    // assume the bottom corresponds to y=1.
    // return:  array of y values for the tics.
    //-------------------------------------------------------------
    getLogYTics: function() {
      let v = [];
      let yh = this.yhi();
      for (let y = 1; y < yh; y *= 10) v.push(y);
      return v;
    },

    //-------------------------------------------------------------
    // rebin so that the new histogram has the maximum bins specified.
    // Since rebinning will only combine integer multiples,
    // the final histogram may have fewer bins than the maximum.
    //-------------------------------------------------------------
    rebin: function(maxbins) {
      if (maxbins >= this.nbins()) {
        // don't modify if histogram bins already fit within maximum
        return this;
      } else {
        const factor = Math.ceil(this.nbins() / maxbins);
        const nb = Math.ceil(this.nbins() / factor); // new xhi >= old xhi
        const olddx = this.dx();
        const ndata = new Array(nb);
        const xwidth = Math.round(factor * olddx * nb);
        let j = 0;
        let k = 0;
        ndata[0] = 0;
        for (let i = 0; i < this.nbins(); i++) {
          ndata[j] += this.data[i];
          if (++k == factor) {
            k = 0;
            j += 1;
            ndata[j] = 0;
          }
        }
        //console.log('rebin: factor=' + factor + ' nb=' + nb +
        //            ' olddx=' + olddx);
        //console.log('rebin: xlo=' + this.xlo + ' xwidth=' + xwidth +
        //            ' new nbins=' + ndata.length);
        nh = new $.Histogram(ndata, this.xlo, this.xlo + xwidth)
        return nh;
      }
    },

    //-------------------------------------------------------------
    // return a new histogram with empty bins removed above and below.
    // xlo and xhi are adjusted accordingly.
    //-------------------------------------------------------------
    trim: function() {
      let ilo = 0;
      while (this.data[ilo] == 0 && ilo < this.data.length) ilo += 1;
      if (ilo == this.data.length) {
        // empty histogram
        return new $.Histogram([], this.xlo, this.xhi);
      }
      let ihi = this.data.length - 1;
      while (this.data[ihi] == 0) ihi -= 1; // don't need bound check here
      ihi += 1; // exclusive upper end
      const d = this.dx();
      //console.log('trim: dx=' + d);
      return new $.Histogram(this.data.slice(ilo, ihi),
                             this.xlo + ilo*d, this.xlo + ihi*d);
    },

    //-------------------------------------------------------------
    // return a stretcher function derived from the cdf.
    //
    // This should work for dx > 1, but usually the stretcher
    // will operate on histograms with dx=1.  Rebinning is
    // usually for display purposes, not for actual color-mapping.
    //-------------------------------------------------------------
    makeStretcher: function(colormap) {
      const sh = this.trim(); // trim zero bins off sides
      let sum = 0;
      let a = new Array(sh.nbins());
      for (let i = 0; i < sh.nbins(); i++) {
        sum += sh.data[i];
        a[i] = sum;
      }
      const range = sum + 1; // compress cdf to range 0..1
      let rm = new Array(sh.nbins());
      let gm = new Array(sh.nbins());
      let bm = new Array(sh.nbins());
      for (let i = 0; i < sh.nbins(); i++) {
        v = colormap(a[i] / range);
        rm[i] = v[0];
        gm[i] = v[1];
        bm[i] = v[2];
      }
      return (function(cs) {
        return function(v) {
          if (v < cs.xlo) return $.colormaps.underflow;
          if (v >= cs.xhi) return $.colormaps.overflow;
          const i = ((v - cs.xlo) / cs.dx()) | 0;
          return [ rm[i], gm[i], bm[i], 255 ];
        }
      })(this);
    }

  };

  //===============================================================
  // histogram renderer
  //===============================================================

  //---------------------------------------------------------------
  // constructor
  //   hist = Histogram object (will be trimmed and rebinned to fit canvas)
  //   width = total width in pixels of the canvas to paint
  //   height = total height in pixels of the canvas to paint
  //   margin = fraction of width and height to devote to left
  //            and bottom margins
  //   barHeight = fraction of height to devote to colorbar
  //
  // After construction, can change the following parameters:
  //   font = font for axis labels, default "10px Arial"
  //   strokeStyle = stroke style, default "black"
  //   lineWidth = line width in pixels, default 1
  //   barSpacer = pixel space between histogram and bar (default 2)
  //   logy = true if vertical axis in log scale, false if linear
  //---------------------------------------------------------------
  $.Renderer = function(hist, width, height, margin, barHeight) {
    this.width = width; // total width (margin + hist)
    this.height = height; // total height (hist + bar + margin)
    this.margin = margin; // portion of canvas width to reserve on left/bottom
    this.barHeight = barHeight; // portion of height to reserve for colorbar

    // style settings which can be modified before drawing
    this.font = "10px Arial";
    this.strokeStyle = "black";
    this.lineWidth = 1;

    // renderer style settings
    this.barSpacer = 2; // two pixel spacer between histogram and bar
    this.logy = true;

    // dimensions of parts
    this.histw = (1.0 - margin) * width | 0;
    this.histh = (1.0 - margin) * height | 0;
    this.xbase = width - this.histw;

    this.hist = hist.trim().rebin(this.histw); // trim and rebin for rendering
 }

  $.Renderer.prototype = {

    //-------------------------------------------------------------
    // draw horizontal colorbar
    //   pxl = image data array
    //   width = width in pixels of pxl
    //   height = height in pixels of pxl
    //   stretcher = function (scalar -> rgba)
    //-------------------------------------------------------------
    horizontalBar: function(pxl, width, height, stretcher) {
      const n = Math.min(width, this.hist.nbins());
      const d = this.hist.dx();
      for (let i = 0; i < n; i++) {
        let v = i * d + this.hist.xlo;
        let c = stretcher(v);
        //console.log('horizontalBar: i=' + i + ' v=' + v +
        //            ' r=' + c[0] + ' g=' + c[1] + ' b=' + c[2] + ' a=' + c[3]);
        let p = i * 4;
        pxl[p] = c[0];
        pxl[p+1] = c[1];
        pxl[p+2] = c[2];
        pxl[p+3] = c[3];
      }
      for (let j = 1; j < height; j++) {
        let p = j * width * 4;
        for (let i = 0; i < 4*n; i++) pxl[p+i] = pxl[i];
      }
    },

    //-------------------------------------------------------------
    // draw histogram
    //   pxl = image data array
    //   width = width in pixels of pxl
    //   height = height in pixels of pxl
    //   stretcher = function (scalar -> rgba)
    //-------------------------------------------------------------
    histogram: function(pxl, width, height, stretcher) {
      const n = Math.min(width, this.hist.nbins());
      const d = this.hist.dx();
      let yhi = this.hist.yhi();
      if (this.logy) yhi = Math.log(yhi);
      for (let i = 0; i < n; i++) {
        let y = this.hist.data[i];
        if (y == 0) continue;
        if (this.logy) y = Math.log(y);
        y = ((y / yhi) * height) | 0;
        let v = i * d + this.hist.xlo;
        for (let p = ((height-y)*width+i)*4; p < 4*width*height; p += 4*width) {
          let c = stretcher(v);
          pxl[p] = c[0];
          pxl[p+1] = c[1];
          pxl[p+2] = c[2];
          pxl[p+3] = c[3];
        }
      }
    },

    //-------------------------------------------------------------
    // return horizontal canvas position in pixels
    //   x = pixel value, which over span of histogram
    //       should lie between xlo and xhi.
    //-------------------------------------------------------------
    getXCoordinate: function(x) {
      return (((x - this.hist.xlo) / this.hist.dx()) + this.xbase) | 0;
      //return ((x - this.hist.xlo) * this.histw /
      //        (this.hist.xhi - this.hist.xlo) | 0) +
      //       this.xbase;
    },

    //-------------------------------------------------------------
    // return the pixel value given the horizontal position in canvas
    //   xc = horizontal position in pixels in canvas
    // return value:  pixel value, between xlo and xhi.
    //-------------------------------------------------------------
    getXPosition: function(xc) {
      return this.hist.xlo + (xc - this.xbase) * this.hist.dx();
    },

    //-------------------------------------------------------------
    // return the vertical position in pixels, assuming 0 is top
    //   y = count of pixels with a particular pixel value.
    //-------------------------------------------------------------
    getYCoordinate: function(y) {
      const yh = this.hist.yhi();
      return (yh - y) * this.histh / yh | 0;
    },

    //-------------------------------------------------------------
    // return the vertical position in pixels of log(y)
    //-------------------------------------------------------------
    getLogYCoordinate: function(y) {
      const maxly = Math.log(this.hist.yhi());
      const ly = Math.log(y);
      return (maxly - ly) * this.histh / maxly | 0;
    },

    //-------------------------------------------------------------
    // get contents of the bin at the pixel location
    //   xc = horizontal position in pixels in canvas
    // rendering assumes one visual pixel column corresponds to one bin,
    // which may have dx > 1.
    //-------------------------------------------------------------
    getContents: function(xc) {
      const i = xc - this.xbase; // one pixel column per bin
      if (i < 0 || i >= this.hist.nbins()) return 0;
      else return this.hist.data[i];
    },

    //-------------------------------------------------------------
    // horizontal axis (no line - assume colorbar serves as line)
    //-------------------------------------------------------------
    horizontalAxis: function(context, xtics, ytop, ybottom) {
      context.textAlign = "center";
      context.textBaseline = "hanging";
      for (let i = 0; i < xtics.length; i++) {
        const v = xtics[i]; // pixel value (x axis)
        const x = this.getXCoordinate(v); // location on canvas
        //console.log('horizontalAxis: x=' + v + ' xc=' + x);
        context.beginPath();
        context.moveTo(x, ytop);
        context.lineTo(x, ybottom);
        context.stroke();
        context.fillText("" + v, x, ybottom);
      }
    },

    //-------------------------------------------------------------
    // vertical axis
    //-------------------------------------------------------------
    verticalAxis: function(context, ytics, ycoord, xleft, xright) {
      context.textAlign = "right";
      for (let i = 0; i < ytics.length; i++) {
        const v = ytics[i]; // count value
        const y = ycoord[i]; // location on canvas
        context.beginPath();
        context.moveTo(xleft, y);
        context.lineTo(xright, y);
        context.stroke();
        context.fillText("" + v, xleft, y);
      }
    },

    //-------------------------------------------------------------
    // draw a decorated histogram and colorbar on the canvas
    //-------------------------------------------------------------
    draw: function(context, stretcher) {
      context.font = this.font;
      context.strokeStyle = this.strokeStyle;
      context.lineWidth = this.lineWidth;

      let img = context.createImageData(this.histw, this.histh);
      for (let i = 0; i < img.data.length; i++) img.data[i] = 0;
      this.histogram(img.data, this.histw, this.histh, stretcher);
      context.putImageData(img, this.xbase, 0);

      // height of colorbar in pixels
      const barh = (this.barHeight * this.height - this.barSpacer) | 0;
      if (this.barHeight > 0) {
        let cbimg = context.createImageData(this.histw, barh);
        this.horizontalBar(cbimg.data, this.histw, barh, stretcher);
        context.putImageData(cbimg, this.xbase, this.histh + this.barSpacer);
      }

      // horizontal axis (assume colorbar serves as line)
      //console.log("horizontal axis dx=" + this.hist.dx());
      const xtics = this.hist.getXTics();
      this.horizontalAxis(context, xtics,
                          this.histh + barh + this.barSpacer,
                          this.histh + barh + this.barSpacer + ((barh/2)|0));

      // vertical axis:  linear or log
      let ytics = null;
      let ycoord = null;
      if (this.logy) {
        ytics = this.hist.getLogYTics();
        ycoord = new Array(ytics.length);
        for (let i = 0; i < ytics.length; i++) {
          ycoord[i] = this.getLogYCoordinate(ytics[i]);
        }
      } else {
        ytics = this.hist.getYTics();
        ycoord = new Array(ytics.length);
        for (let i = 0; i < ytics.length; i++) {
          ycoord[i] = this.getYCoordinate(ytics[i]);
        }
      }
      this.verticalAxis(context, ytics, ycoord,
                        (0.9 * this.margin * this.width) | 0,
                        (this.margin * this.width) | 0);

    },

    //-------------------------------------------------------------
    // return information based on x position in histogram
    //   x = horizontal pixel location within canvas
    //-------------------------------------------------------------
    info: function(x) {
      const v = this.getXPosition(x);
      if (v < this.xlo || v >= this.xhi) return { valid: false }
      return {
        valid: true,
        value: v,
        counts: this.getContents(x) // contents of bin (after rebinning)
      };
    }

  };

  //===============================================================
  // stretches:  functions which map pixel value -> rgba
  //
  // The functions return a function which
  // can be used by a filter to apply to pixel values.
  //===============================================================
  $.stretches = {};

  //---------------------------------------------------------------
  // generic stretch function, which takes a pixel value
  // and returns rgba
  //---------------------------------------------------------------
  $.stretches.generic = function(stretcher, colormap) {
    return function(v) { return colormap(stretcher(v)); }
  }

  //---------------------------------------------------------------
  // shortcut for histogram-based auto-stretch.
  // If you want access to the histogram or colorbar,
  // then use the $.Histogram class directly.
  //---------------------------------------------------------------
  $.stretches.histogram = function(bins, xlo, xhi, colormap) {
    const hist = new $.Histogram(bins, xlo, xhi).trim();
    return hist.makeStretcher(colormap);
  }

  //===============================================================
  // filter utilities
  //===============================================================
  $.filter = {};

  //---------------------------------------------------------------
  // apply the decoder and stretcher to all the pixels
  //   pxl = pixel array, as from context.getImageData(...).data
  //   decoder = function rgba -> scalar pixel value
  //   stretcher = function scalar pixel value -> rgba
  //---------------------------------------------------------------
  $.filter.apply = function(pxl, decoder, stretcher) {
    for (let i = 0; i < pxl.length; i += 4) {
      let v = decoder(pxl[i], pxl[i+1], pxl[i+2], pxl[i+3]);
      let c = stretcher(v);
      pxl[i] = c[0];
      pxl[i+1] = c[1];
      pxl[i+2] = c[2];
      pxl[i+3] = c[3];
    }
  };

  //---------------------------------------------------------------
  // make a histogram based on given pixels
  //   pxl = pixel array, as from context.getImageData(...).data
  //   decoder = function rgba -> scalar pixel value
  //---------------------------------------------------------------
  $.filter.accumulate = function(pxl, decoder) {
    let xmin = 1 << 18;
    let xmax = 0;
    let counts = new Array(1 << 18);
    for (let i = 0; i < counts.length; i++) counts[i] = 0;
    for (let i = 0; i < pxl.length; i += 4) {
      let v = decoder(pxl[i], pxl[i+1], pxl[i+2], pxl[i+3]);
      counts[v] += 1;
      if (v > xmax) xmax = v;
      if (v < xmin) xmin = v;
    }
    let bins = new Array(xmax - xmin + 1);
    let j = 0;
    for (let i = xmin; i <= xmax; i++) bins[j++] = counts[i];
    let hist = new ColorStretch.Histogram(bins, xmin, xmax+1);
    return hist;
  };


})(ColorStretch);

