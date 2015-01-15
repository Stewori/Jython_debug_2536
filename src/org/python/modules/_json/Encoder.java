package org.python.modules._json;

import org.python.core.ArgParser;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyLong;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PyTuple;
import org.python.core.PyType;
import org.python.core.PyUnicode;
import org.python.expose.ExposedGet;
import org.python.expose.ExposedType;

@ExposedType(name = "_json.encoder", base = PyObject.class)
public class Encoder extends PyObject {

    public static final PyType TYPE = PyType.fromClass(Encoder.class);

    @ExposedGet
    public final String __module__ = "_json";

    final PyDictionary markers;
    final PyObject defaultfn;
    final PyObject encoder;
    final PyObject indent;
    final PyObject key_separator;
    final PyObject item_separator;
    final PyObject sort_keys;
    final boolean skipkeys;
    final boolean allow_nan;

    public Encoder(PyObject[] args, String[] kwds) {
        super();
        ArgParser ap = new ArgParser("encoder", args, kwds,
                new String[]{"markers", "default", "encoder", "indent",
                        "key_separator", "item_separator", "sort_keys", "skipkeys", "allow_nan"});
        ap.noKeywords();
        PyObject m = ap.getPyObject(0);
        markers = m == Py.None ? null : (PyDictionary) m;
        defaultfn = ap.getPyObject(1);
        encoder = ap.getPyObject(2);
        indent = ap.getPyObject(3);
        key_separator = ap.getPyObject(4);
        item_separator = ap.getPyObject(5);
        sort_keys = ap.getPyObject(6);
        skipkeys = ap.getPyObject(7).__nonzero__();
        allow_nan = ap.getPyObject(8).__nonzero__();
    }

    public PyObject __call__(PyObject obj) {
        return __call__(obj, Py.Zero);
    }

    public PyObject __call__(PyObject obj, PyObject indent_level) {
        PyList rval = new PyList();
        encode_obj(rval, obj, 0);
        return rval;
    }

    private PyString encode_float(PyObject obj) {
        /* Return the JSON representation of a PyFloat */
        double i = obj.asDouble();
        if (Double.isInfinite(i) || Double.isNaN(i)) {
            if (!allow_nan) {
                throw Py.ValueError("Out of range float values are not JSON compliant");
            }
            if (i == Double.POSITIVE_INFINITY) {
                return new PyString("Infinity");
            } else if (i == Double.NEGATIVE_INFINITY) {
                return new PyString("-Infinity");
            } else {
                return new PyString("NaN");
            }
        }
        /* Use a better float format here? */
        return obj.__repr__();
    }

    private PyString encode_string(PyObject obj) {
        /* Return the JSON representation of a string */
        return (PyString) encoder.__call__(obj);
    }

    private PyObject checkCircularReference(PyObject obj) {
        PyObject ident = null;
        if (markers != null) {
            ident = Py.newInteger(Py.id(obj));
            if (markers.__contains__(ident)) {
                throw Py.ValueError("Circular reference detected");
            }
            markers.__setitem__(ident, obj);
        }
        return ident;
    }

    private void encode_obj(PyList rval, PyObject obj, int indent_level) {
        /* Encode Python object obj to a JSON term, rval is a PyList */
        if (obj == Py.None) {
            rval.append(new PyString("null"));
        } else if (obj == Py.True) {
            rval.append(new PyString("true"));
        } else if (obj == Py.False) {
            rval.append(new PyString("false"));
        } else if (obj instanceof PyString) {
            rval.append(encode_string(obj));
        } else if (obj instanceof PyInteger || obj instanceof PyLong) {
            rval.append(obj.__str__());
        } else if (obj instanceof PyFloat) {
            rval.append(encode_float(obj));
        } else if (obj instanceof PyList || obj instanceof PyTuple) {
            encode_list(rval, obj, indent_level);
        } else if (obj instanceof PyDictionary) {
            encode_dict(rval, (PyDictionary) obj, indent_level);
        } else {
            PyObject ident = checkCircularReference(obj);
            if (defaultfn == Py.None) {
                throw Py.TypeError(String.format(".80s is not JSON serializable", obj.__repr__()));
            }

            PyObject newobj = defaultfn.__call__(obj);
            encode_obj(rval, newobj, indent_level);
            if (ident != null) {
                markers.__delitem__(ident);
            }
        }
    }

    private void encode_dict(PyList rval, PyDictionary dct, int indent_level) {
        /* Encode Python dict dct a JSON term */
        if (dct.__len__() == 0) {
            rval.append(new PyString("{}"));
            return;
        }

        PyObject ident = checkCircularReference(dct);
        rval.append(new PyString("{"));

        /* TODO: C speedup not implemented for sort_keys */

        int idx = 0;
        for (PyObject key : dct.asIterable()) {
            PyString kstr;

            if (key instanceof PyString || key instanceof PyUnicode) {
                kstr = (PyString) key;
            } else if (key instanceof PyFloat) {
                kstr = encode_float(key);
            } else if (key instanceof PyInteger || key instanceof PyLong) {
                kstr = key.__str__();
            } else if (key == Py.True) {
                kstr = new PyString("true");
            } else if (key == Py.False) {
                kstr = new PyString("false");
            } else if (key == Py.None) {
                kstr = new PyString("null");
            } else if (skipkeys) {
                continue;
            } else {
                throw Py.TypeError(String.format("keys must be a string: %.80s", key.__repr__()));
            }

            if (idx > 0) {
                rval.append(item_separator);
            }

            PyObject value = dct.__getitem__(key);
            PyString encoded = encode_string(kstr);
            rval.append(encoded);
            rval.append(key_separator);
            encode_obj(rval, value, indent_level);
            idx += 1;
        }

        if (ident != null) {
            markers.__delitem__(ident);
        }
        rval.append(new PyString("}"));
    }


    private void encode_list(PyList rval, PyObject seq, int indent_level) {
        PyObject ident = checkCircularReference(seq);
        rval.append(new PyString("["));

        int i = 0;
        for (PyObject obj : seq.asIterable()) {
            if (i > 0) {
                rval.append(item_separator);
            }
            encode_obj(rval, obj, indent_level);
            i++;
        }

        if (ident != null) {
            markers.__delitem__(ident);
        }
        rval.append(new PyString("]"));
    }
}